package com.pocketring;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class PocketRingItem extends Item {

    public static final ResourceKey<Level> POCKET = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(PocketRing.MOD_ID, "pocket"));

    private static final String RETURN_TAG = "pocketring_return";
    // Chao do mundo plano: bedrock y=0, deepslate y=1..3 -> jogador fica em y=4
    private static final double POCKET_X = 0.5, POCKET_Y = 4.0, POCKET_Z = 0.5;
    private static final int COOLDOWN_TICKS = 40;

    public PocketRingItem(Properties properties) {
        super(properties);
    }

    /** Botao direito em um bloco (chao). */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (level.dimension().equals(POCKET)) {
                leavePocket(sp);
            } else {
                BlockPos above = ctx.getClickedPos().relative(ctx.getClickedFace());
                enterPocket(sp, (ServerLevel) level, Vec3.atBottomCenterOf(above));
            }
            sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Botao direito no ar: so serve para sair (dentro do bolso nao ha "chao" obrigatorio). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.dimension().equals(POCKET)) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                leavePocket(sp);
                sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private void enterPocket(ServerPlayer player, ServerLevel from, Vec3 ringPos) {
        ServerLevel pocket = player.server.getLevel(POCKET);
        if (pocket == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Dimensao de bolso nao encontrada!"), true);
            return;
        }

        // guarda de onde o jogador veio
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", from.dimension().location().toString());
        tag.putDouble("x", player.getX());
        tag.putDouble("y", player.getY());
        tag.putDouble("z", player.getZ());
        tag.putFloat("yRot", player.getYRot());
        tag.putFloat("xRot", player.getXRot());
        player.getPersistentData().put(RETURN_TAG, tag);

        fireCircle(from, ringPos);
        player.teleportTo(pocket, POCKET_X, POCKET_Y, POCKET_Z, Set.of(), player.getYRot(), 0f);
        fireCircle(pocket, new Vec3(POCKET_X, POCKET_Y, POCKET_Z));
    }

    private void leavePocket(ServerPlayer player) {
        ServerLevel target = player.server.overworld();
        double x, y, z;
        float yRot = player.getYRot(), xRot = 0f;

        CompoundTag tag = player.getPersistentData().getCompound(RETURN_TAG);
        if (!tag.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("dim"));
            if (id != null) {
                ServerLevel found = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                if (found != null) target = found;
            }
            x = tag.getDouble("x");
            y = tag.getDouble("y");
            z = tag.getDouble("z");
            yRot = tag.getFloat("yRot");
            xRot = tag.getFloat("xRot");
        } else {
            // fallback (ex.: anel usado depois de perder os dados): spawn do mundo
            BlockPos spawn = target.getSharedSpawnPos();
            x = spawn.getX() + 0.5;
            y = spawn.getY();
            z = spawn.getZ() + 0.5;
        }
        player.getPersistentData().remove(RETURN_TAG);

        fireCircle((ServerLevel) player.level(), player.position());
        player.teleportTo(target, x, y, z, Set.of(), yRot, xRot);
        fireCircle(target, new Vec3(x, y, z));
    }

    /** Circulo de fogo feito de particulas + som. */
    private static void fireCircle(ServerLevel level, Vec3 center) {
        final double radius = 1.8;
        final int points = 48;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.FLAME, px, center.y + 0.1, pz, 3, 0.05, 0.25, 0.05, 0.01);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.LAVA, px, center.y + 0.2, pz, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
