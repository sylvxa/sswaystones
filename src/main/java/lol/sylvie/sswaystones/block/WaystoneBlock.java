/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import lol.sylvie.sswaystones.gui.ViewerUtil;
import lol.sylvie.sswaystones.storage.PlayerData;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.HashUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class WaystoneBlock extends BaseEntityBlock implements PolymerBlock {
    private final WaystoneStyle style;

    public WaystoneBlock(WaystoneStyle style, Properties settings) {
        super(settings);
        this.style = style;
    }

    public WaystoneStyle getStyle() {
        return style;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec((settings) -> new WaystoneBlock(style, settings));
    }

    // Visuals
    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext packetContext) {
        return this.style.getWall().defaultBlockState().setValue(WallBlock.UP, true)
                .setValue(WallBlock.EAST, WallSide.LOW).setValue(WallBlock.WEST, WallSide.LOW)
                .setValue(WallBlock.NORTH, WallSide.LOW).setValue(WallBlock.SOUTH, WallSide.LOW);
    }

    // Should be indestructible by TNT, also lets me ignore some edge cases.
    @Override
    public float getExplosionResistance() {
        return 1200;
    }

    private static WaystoneRecord createWaystone(BlockPos pos, Level world, ServerPlayer player) {
        assert world.getServer() != null;
        WaystoneStorage storage = WaystoneStorage.getServerState(world.getServer());
        return storage.createWaystone(pos, world, player);
    }

    private static WaystoneRecord getWaystone(BlockPos pos, ServerLevel world) {
        WaystoneStorage storage = WaystoneStorage.getServerState(world.getServer());
        return storage.getWaystone(HashUtil.getHash(pos, world.dimension()));
    }

    // Placing & breaking
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);

        if (world.isClientSide() || placer == null)
            return;
        if (!(placer instanceof ServerPlayer player))
            return;
        createWaystone(pos, world, player);
    }

    public static void onRemoved(Level world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        assert server != null;

        WaystoneStorage storage = WaystoneStorage.getServerState(server);
        WaystoneRecord record = getWaystone(pos, (ServerLevel) world);

        if (world.getBlockEntity(pos) instanceof WaystoneBlockEntity waystoneBlockEntity) {
            waystoneBlockEntity.removeDisplay();
        }

        if (record != null)
            storage.destroyWaystone(record);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide())
            onRemoved(world, pos);
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        WaystoneRecord record = getWaystone(pos, (ServerLevel) world);
        if (record == null || Permissions.check(player, "sswaystones.create.server", 4)) {
            return super.getDestroyProgress(state, player, world, pos);
        }

        return record.getAccessSettings().isServerOwned() ? 0 : super.getDestroyProgress(state, player, world, pos);
    }

    // Open GUI
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.level().getServer();
            WaystoneStorage storage = WaystoneStorage.getServerState(server);
            PlayerData playerData = WaystoneStorage.getPlayerState(serverPlayer);

            // Make sure we remember it!
            String waystoneHash = HashUtil.getHash(pos, world.dimension());
            WaystoneRecord record = storage.getWaystone(waystoneHash);

            if (record == null) {
                WaystoneRecord newWaystone = createWaystone(pos, world, serverPlayer);
                if (newWaystone == null)
                    return InteractionResult.FAIL;
                record = newWaystone;
            }

            if (!playerData.discoveredWaystones.contains(waystoneHash)) {
                playerData.discoveredWaystones.add(waystoneHash);
                player.displayClientMessage(Component
                        .translatable("message.sswaystones.discovered",
                                record.getWaystoneText().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.DARK_PURPLE), false);
            }

            ViewerUtil.openGui(serverPlayer, record);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    // Block entity
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaystoneBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
            BlockEntityType<T> type) {
        if (type.equals(ModBlocks.WAYSTONE_BLOCK_ENTITY)) {
            return (tickerWorld, pos, tickerState, blockEntity) -> WaystoneBlockEntity.tick(tickerWorld,
                    (WaystoneBlockEntity) blockEntity);
        }
        return null;
    }
}
