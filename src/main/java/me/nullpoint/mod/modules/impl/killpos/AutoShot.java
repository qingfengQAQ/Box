package me.nullpoint.mod.modules.impl.killpos;

import me.nullpoint.api.managers.CommandManager;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoShot extends Module {
    private final BooleanSetting requireClearView = new BooleanSetting("ClearView", true);
    private final BooleanSetting fastShot = new BooleanSetting("FastShot", true);
    private int usingTicks = 0;
    private int cooldown = 0;

    public AutoShot() {
        super("AutoShot", Category.KillPos);
        addSetting(requireClearView);
        addSetting(fastShot);
    }

    @Override
    public void onEnable() {
        usingTicks = 0;
        cooldown = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        // 冷却计时
        if (cooldown > 0) {
            cooldown--;
        }

        // 获取 AutoAim 实例
        AutoAim autoAim = AutoAim.INSTANCE;
        if (autoAim == null || !autoAim.isOn()) {
            CommandManager.sendChatMessage("\u00a7e[?] \u00a7c\u00a7oAutoAim?");
            this.disable();
            return;
        }

        // 使用 hasTarget() 方法检查是否有有效目标
        if (!autoAim.hasTarget()) {
            // 没有目标，重置蓄力
            if (mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(false);
            }
            usingTicks = 0;
            return;
        }

        Entity target = autoAim.getCurrentTarget();
        if (target == null) {
            // 额外检查以防万一
            if (mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(false);
            }
            usingTicks = 0;
            return;
        }

        // 计算目标距离
        double distance = mc.player.distanceTo(target);

        // 检查玩家手中拿的是弓还是弩
        Item mainHandItem = mc.player.getMainHandStack().getItem();
        boolean isBow = mainHandItem instanceof BowItem;
        boolean isCrossbow = mainHandItem instanceof CrossbowItem;
        if (!isBow && !isCrossbow) {
            // 如果不是弓也不是弩，则重置并返回
            if (mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(false);
            }
            usingTicks = 0;
            return;
        }

        // 检查是否有障碍物（如果requireClearView为true）
        if (requireClearView.getValue()) {
            Vec3d start = mc.player.getEyePos();
            Vec3d end = autoAim.getTargetPosition(target); // 使用AutoAim的目标位置

            // 创建碰撞箱
            Box collisionBox = new Box(
                    Math.min(start.x, end.x),
                    Math.min(start.y, end.y),
                    Math.min(start.z, end.z),
                    Math.max(start.x, end.x),
                    Math.max(start.y, end.y),
                    Math.max(start.z, end.z)
            ).expand(0.1);

            // 射线检测
            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                    start, end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                // 有障碍物，进行蓄力（但不发射）
                if (!mc.player.isUsingItem()) {
                    // 开始蓄力
                    mc.options.useKey.setPressed(true);
                } else {
                    // 保持蓄力
                    usingTicks++;
                }
                return;
            }
        }

        // 无障碍物，则根据武器类型处理
        if (isBow) {
            handleBowShooting(distance);
        } else if (isCrossbow) {
            handleCrossbowShooting(distance);
        }
    }

    // 处理弓的射击逻辑
    private void handleBowShooting(double distance) {
        if (fastShot.getValue() && distance <= 3.0 && cooldown == 30) {
            // 快速射击模式（距离<=3格）
            if (!mc.player.isUsingItem()) {
                // 开始蓄力
                mc.options.useKey.setPressed(true);
                usingTicks = 0;
            } else {
                // 立即发射
                mc.options.useKey.setPressed(false);
                usingTicks = 0;
                cooldown = 30; // 设置冷却防止过于频繁
            }
        } else {
            // 正常蓄力模式
            if (!mc.player.isUsingItem()) {
                // 开始蓄力
                mc.options.useKey.setPressed(true);
                usingTicks = 0;
            } else {
                usingTicks++;
                // 蓄力至少20tick后发射（满蓄力）
                if (usingTicks >= 20) {
                    mc.options.useKey.setPressed(false);
                    usingTicks = 0; // 重置
                }
            }
        }
    }

    // 处理弩的射击逻辑
    private void handleCrossbowShooting(double distance) {
        if (fastShot.getValue() && distance <= 3.0 && cooldown == 0) {
            // 快速射击模式（距离<=3格）
            if (!mc.player.isUsingItem()) {
                // 按下右键
                mc.options.useKey.setPressed(true);
            } else {
                // 立即释放
                mc.options.useKey.setPressed(false);
                cooldown = 3; // 设置冷却防止过于频繁
            }
        } else {
            // 正常射击模式
            if (!mc.player.isUsingItem()) {
                // 第一帧：按下右键
                mc.options.useKey.setPressed(true);
            } else {
                // 第二帧：释放右键
                mc.options.useKey.setPressed(false);
            }
        }
    }
}