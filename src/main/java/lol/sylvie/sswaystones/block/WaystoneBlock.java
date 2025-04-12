/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.gui.ViewerUtil;
import lol.sylvie.sswaystones.storage.PlayerData;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallShape;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class WaystoneBlock extends BlockWithEntity implements PolymerBlock {
    public static final Identifier ID = Waystones.id("waystone");
    public static final RegistryKey<Block> KEY = RegistryKey.of(Registries.BLOCK.getKey(), ID);

    public WaystoneBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(WaystoneBlock::new);
    }

    // Visuals
    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext packetContext) {
        return Blocks.STONE_BRICK_WALL.getDefaultState().with(WallBlock.UP, true)
                .with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW)
                .with(WallBlock.NORTH_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW);
    }

    // Should be indestructible by TNT, also lets me ignore some edge cases.
    @Override
    public float getBlastResistance() {
        return 1200;
    }

    private WaystoneRecord makeWaystoneHere(BlockPos pos, World world, LivingEntity owner) {
        assert world.getServer() != null;
        WaystoneStorage storage = WaystoneStorage.getServerState(world.getServer());
        return storage.createWaystone(pos, world, owner);
    }

    // Placing & breaking
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (world.isClient || placer == null)
            return;
        makeWaystoneHere(pos, world, placer);
    }

    private void onRemoved(World world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        assert server != null;

        WaystoneStorage storage = WaystoneStorage.getServerState(server);
        WaystoneRecord record = storage.getWaystone(HashUtil.getHash(pos, world.getRegistryKey()));

        if (world.getBlockEntity(pos) instanceof WaystoneBlockEntity waystoneBlockEntity) {
            waystoneBlockEntity.removeDisplay();
        }

        if (record != null)
            storage.destroyWaystone(server, record);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient())
            onRemoved(world, pos);
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(ModBlocks.WAYSTONE) && !world.isClient())
            onRemoved(world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // Open GUI
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            assert serverPlayer.getServer() != null;
            WaystoneStorage storage = WaystoneStorage.getServerState(serverPlayer.getServer());
            PlayerData playerData = WaystoneStorage.getPlayerState(serverPlayer);

            // Make sure we remember it!
            String waystoneHash = HashUtil.getHash(pos, world.getRegistryKey());
            WaystoneRecord record = storage.getWaystone(waystoneHash);

            if (record == null) {
                record = makeWaystoneHere(pos, world, serverPlayer);
            }

            if (!playerData.discoveredWaystones.contains(waystoneHash)) {
                playerData.discoveredWaystones.add(waystoneHash);
                player.sendMessage(Text
                        .translatable("message.sswaystones.discovered",
                                record.getWaystoneText().copy().formatted(Formatting.BOLD, Formatting.GOLD))
                        .formatted(Formatting.DARK_PURPLE), false);
            }

            ViewerUtil.openGui(serverPlayer, record);
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hit);
    }

    // Block entity
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WaystoneBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> type) {
        if (type.equals(ModBlocks.WAYSTONE_BLOCK_ENTITY)) {
            return (tickerWorld, pos, tickerState, blockEntity) -> WaystoneBlockEntity.tick(tickerWorld,
                    (WaystoneBlockEntity) blockEntity);
        }
        return null;
    }
}
