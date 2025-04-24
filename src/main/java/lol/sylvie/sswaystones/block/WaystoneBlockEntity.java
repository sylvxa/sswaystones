/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import java.awt.*;
import java.util.Random;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class WaystoneBlockEntity extends BlockEntity {
    private static final Random RANDOM = new Random();
    private final ElementHolder holder = new ElementHolder();
    private ChunkAttachment attachment;

    public TextDisplayElement nameDisplay = null;
    public ItemDisplayElement eyeDisplay = null;

    public WaystoneRecord waystone;

    public WaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WAYSTONE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, WaystoneBlockEntity waystoneEntity) {
        WaystoneRecord record = waystoneEntity.getThisWaystone(world);
        boolean shouldCreateName = record != null && waystoneEntity.nameDisplay == null;

        Formatting color = Formatting.RESET;
        if (record != null) {
            String teamName = record.getAccessSettings().getTeam();
            if (!teamName.isEmpty()) {
                Team team = world.getScoreboard().getTeam(teamName);
                if (team != null) {
                    color = team.getColor();
                }
            }
        }

        if (waystoneEntity.eyeDisplay == null || shouldCreateName) {
            waystoneEntity.createHologramDisplay(world);
        }

        // Eye rotation
        waystoneEntity.eyeDisplay.setYaw(waystoneEntity.eyeDisplay.getYaw() + 4f);

        // Particles
        if (RANDOM.nextInt(0, 20) == 0 && world instanceof ServerWorld serverWorld) {
            Vec3d pos = waystoneEntity.getPos().add(0, 1, 0).toCenterPos();
            Integer colorValue = color.getColorValue();
            serverWorld.spawnParticles(
                    new DustParticleEffect(
                            color == Formatting.RESET || colorValue == null ? Color.RED.getRGB() : colorValue, 1f),
                    pos.getX(), pos.getY(), pos.getZ(), 8, 0.1d, 0.1d, 0.1d, 0.1d);
        }

        // Waystone title
        if (waystoneEntity.nameDisplay == null)
            return;

        waystoneEntity.nameDisplay.setText(record.getWaystoneText().copy().formatted(color));

        // Bob up and down
        double y = (Math.sin((double) System.currentTimeMillis() / 1000) / 32) + 1.55d;
        waystoneEntity.nameDisplay.setOffset(new Vec3d(0, y, 0));
    }

    private @Nullable WaystoneRecord getThisWaystone(World world) {
        if (world.isClient())
            return null;
        assert world.getServer() != null; // World can't be client.

        // Grab waystone if it isn't there
        if (this.waystone == null) {
            WaystoneStorage storage = WaystoneStorage.getServerState(world.getServer());
            this.waystone = storage.getWaystone(HashUtil.getHash(pos, world.getRegistryKey()));
        }

        // This can still be null!
        return this.waystone;
    }

    public void createHologramDisplay(World world) {
        this.removeDisplay();

        // Eye display
        ItemStack stack = Items.ENDER_EYE.getDefaultStack();
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        eyeDisplay = new ItemDisplayElement(stack);
        eyeDisplay.setOffset(new Vec3d(0, 1.125, 0));
        eyeDisplay.setScale(new Vector3f(0.75f, 0.75f, 0.75f));
        eyeDisplay.setInterpolationDuration(2);

        holder.addElement(eyeDisplay);

        // Waystone name display
        WaystoneRecord record = getThisWaystone(world);
        if (record != null) {
            nameDisplay = new TextDisplayElement();

            nameDisplay.setText(record.getWaystoneText());
            nameDisplay.setTextAlignment(DisplayEntity.TextDisplayEntity.TextAlignment.CENTER);
            nameDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);

            holder.addElement(nameDisplay);
        }

        attachment = (ChunkAttachment) ChunkAttachment.ofTicking(holder, (ServerWorld) world, pos);
    }

    public void removeDisplay() {
        if (eyeDisplay != null)
            holder.removeElement(eyeDisplay);
        if (nameDisplay != null)
            holder.removeElement(nameDisplay);
        if (attachment != null)
            attachment.destroy();
    }
}
