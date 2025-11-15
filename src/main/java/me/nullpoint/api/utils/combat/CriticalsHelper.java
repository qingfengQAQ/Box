package me.nullpoint.api.utils.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.lang.reflect.Field;

public class CriticalsHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        if (mc.player == null) return InteractType.INTERACT;
        HitResult hit = mc.player.raycast(6.0, mc.getTickDelta(), false);
        return (hit instanceof EntityHitResult) ? InteractType.ATTACK : InteractType.INTERACT;
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        try {
            // 反射访问字段（适配 Yarn 映射）
            Field entityIdField = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            entityIdField.setAccessible(true);
            int entityId = (int) entityIdField.get(packet);
            return mc.world.getEntityById(entityId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum InteractType {
        ATTACK, INTERACT
    }
}