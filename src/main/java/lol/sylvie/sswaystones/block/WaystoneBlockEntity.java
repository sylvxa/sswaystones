package lol.sylvie.sswaystones.block;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.EntityElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import lol.sylvie.sswaystones.storage.WaystoneStorage;
import lol.sylvie.sswaystones.util.HashUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.Random;

public class WaystoneBlockEntity extends BlockEntity {
    private static final Random RANDOM = new Random();
    private final ElementHolder holder = new ElementHolder();
    private HolderAttachment attachment;
    public EntityElement<ArmorStandEntity> hologramDisplay = null;
    public ItemDisplayElement eyeDisplay = null;

    public WaystoneRecord waystone;

    public WaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WAYSTONE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, WaystoneBlockEntity waystoneBlockEntity) {
        // Text display
        if (waystoneBlockEntity.hologramDisplay == null || waystoneBlockEntity.eyeDisplay == null) {
            waystoneBlockEntity.createHologramDisplay(world);
            return;
        }

        // Make sure title updates accordingly
        waystoneBlockEntity.hologramDisplay.entity().setCustomName(Text.literal(waystoneBlockEntity.waystone.getWaystoneName()));

        // Bob up and down
        double y = (Math.sin((double) System.currentTimeMillis() / 1000) / 16) + 0.25d;
        waystoneBlockEntity.hologramDisplay.setOffset(new Vec3d(0, y, 0));

        // Eye rotation
        waystoneBlockEntity.eyeDisplay.setYaw(waystoneBlockEntity.eyeDisplay.getYaw() + 4f);

        // Particles
        if (RANDOM.nextInt(0, 20) == 0 && world instanceof ServerWorld serverWorld) {
            Vec3d pos = waystoneBlockEntity.getPos().add(0, 1, 0).toCenterPos();
            serverWorld.spawnParticles(new DustParticleEffect(new Vector3f(1f, 0f, 0f), 1f), pos.getX(), pos.getY(), pos.getZ(), 8, 0.1d, 0.1d, 0.1d, 0.1d);
        }
    }

    private WaystoneRecord getThisWaystone(World world) {
        if (world.isClient()) return null;
        assert world.getServer() != null; // World can't be client.

        // Grab waystone if it isn't there
        if (this.waystone == null) {
            WaystoneStorage storage = WaystoneStorage.getServerState(world.getServer());
            this.waystone = storage.getWaystone(HashUtil.getHash(pos, world.getRegistryKey()));
        }

        // If it is STILL null
        if (this.waystone == null) {
            Waystones.LOGGER.warn("Someone nuked data they weren't supposed to nuke. :P");
            world.breakBlock(pos, false);
            return null;
        }

        return this.waystone;
    }

    public void createHologramDisplay(World world) {
        WaystoneRecord record = getThisWaystone(world);
        if (record == null) return;

        // Hologram display
        hologramDisplay = new EntityElement<>(EntityType.ARMOR_STAND, (ServerWorld) world);

        ArmorStandEntity entity = hologramDisplay.entity();
        entity.setCustomName(Text.literal(record.getWaystoneName()));
        entity.setCustomNameVisible(true);
        entity.setInvisible(true);
        entity.setNoGravity(true);
        entity.setSmall(true);

        holder.addElement(hologramDisplay);

        // Eye display
        eyeDisplay = new ItemDisplayElement(Items.ENDER_EYE);
        eyeDisplay.setOffset(new Vec3d(0, 1.125, 0));
        eyeDisplay.setScale(new Vector3f(0.75f, 0.75f, 0.75f));

        holder.addElement(eyeDisplay);

        attachment = ChunkAttachment.ofTicking(holder, (ServerWorld) world, pos);
    }
    public void removeDisplay() {
        if (hologramDisplay == null || eyeDisplay == null || attachment == null) return;
        attachment.destroy();
    }
}
