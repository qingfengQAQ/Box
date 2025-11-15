package me.nullpoint.mod.modules.impl.killpos;

import me.nullpoint.api.events.eventbus.EventHandler;
import me.nullpoint.api.events.eventbus.EventPriority;
import me.nullpoint.api.events.impl.RotateEvent;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.stream.StreamSupport;

public class AutoAim extends Module {
    // 静态实例引用
    public static AutoAim INSTANCE;

    // 右键菜单配置项
    private final SliderSetting range = add(new SliderSetting("Range", 6.0, 2.0, 320.0, 0.1));
    private final BooleanSetting lockPlayers = add(new BooleanSetting("Players", true));
    private final BooleanSetting lockMobs = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting lockAnimals = add(new BooleanSetting("Animals", false));
    private final BooleanSetting mouseMode = add(new BooleanSetting("Mouse", false));

    // 瞄准位置配置
    private final BooleanSetting aimAtHead = add(new BooleanSetting("AimHead", true));

    // 自由视角配置
    private final BooleanSetting freeLook = add(new BooleanSetting("FreeLook", true));
    private final SliderSetting lookSpeed = add(new SliderSetting("LookSpeed", 10, 1, 50, 1));

    // 运行时状态
    private Entity currentTarget;
    private float serverYaw, serverPitch;
    private float clientYaw, clientPitch;
    private boolean hasTarget = false; // 新增：是否有有效目标

    public AutoAim() {
        super("AutoAim", Category.KillPos);
        INSTANCE = this; // 初始化静态实例
    }

    @Override
    public void onEnable() {
        // 初始化视角状态
        if (mc.player != null) {
            clientYaw = mc.player.getYaw();
            clientPitch = mc.player.getPitch();
            serverYaw = clientYaw;
            serverPitch = clientPitch;
            hasTarget = false; // 重置目标状态
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRotate(RotateEvent event) {
        if (mc.player == null || mc.world == null) return;

        // 保存客户端视角
        clientYaw = mc.player.getYaw();
        clientPitch = mc.player.getPitch();

        // 寻找并锁定目标
        currentTarget = findTarget();
        hasTarget = (currentTarget != null); // 更新目标状态

        if (hasTarget) {
            aimAtTarget(currentTarget);
        } else {
            // 没有目标时，使用客户端视角作为服务器视角
            serverYaw = clientYaw;
            serverPitch = clientPitch;
        }

        // 应用服务器视角
        event.setYaw(serverYaw);
        event.setPitch(serverPitch);

        // 强制恢复客户端视角 - 确保自由视角始终有效
        if (freeLook.getValue()) {
            mc.player.setYaw(clientYaw);
            mc.player.setPitch(clientPitch);
        }
    }

    // 寻找有效目标
    private Entity findTarget() {
        Vec3d playerPos = mc.player.getEyePos();
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(this::isValidTarget)
                .min(mouseMode.getValue() ?
                        Comparator.comparingDouble(this::getAngleDifference) :
                        Comparator.comparingDouble(e -> e.squaredDistanceTo(playerPos)))
                .orElse(null);
    }

    // 计算目标与准星的角度差
    private double getAngleDifference(Entity target) {
        Vec3d playerEyes = mc.player.getEyePos();
        Vec3d targetPos = getTargetPosition(target);

        // 计算目标方向向量
        Vec3d directionToTarget = targetPos.subtract(playerEyes).normalize();

        // 计算当前视角方向向量
        float yawRad = (float) Math.toRadians(clientYaw);
        float pitchRad = (float) Math.toRadians(clientPitch);
        float cosPitch = MathHelper.cos(pitchRad);
        Vec3d viewDirection = new Vec3d(
                -MathHelper.sin(yawRad) * cosPitch,
                -MathHelper.sin(pitchRad),
                MathHelper.cos(yawRad) * cosPitch
        );

        // 计算角度差的余弦值（点积）
        return 1.0 - viewDirection.dotProduct(directionToTarget);
    }

    // 目标过滤逻辑（增加范围检查）
    private boolean isValidTarget(Entity entity) {
        // 排除自己
        if (entity == mc.player) return false;
        // 排除物品实体
        if (entity instanceof ItemEntity) return false;

        // 范围检查
        double distanceSq = mc.player.squaredDistanceTo(entity);
        double rangeSq = range.getValue() * range.getValue();
        if (distanceSq > rangeSq) {
            return false;
        }

        String typeKey = entity.getType().getTranslationKey();
        return switch (typeKey) {
            case "entity.minecraft.player" -> lockPlayers.getValue();
            case "entity.minecraft.zombie",
                 "entity.minecraft.skeleton" -> lockMobs.getValue();
            case "entity.minecraft.cow",
                 "entity.minecraft.pig" -> lockAnimals.getValue();
            default -> false;
        };
    }

    // 获取目标位置（头部或中心）
    public Vec3d getTargetPosition(Entity target) {
        if (aimAtHead.getValue() && target instanceof LivingEntity) {
            // 瞄准头部（眼睛位置）
            return new Vec3d(
                    target.getX(),
                    target.getY() + target.getEyeHeight(target.getPose()),
                    target.getZ()
            );
        }
        // 瞄准中心
        return target.getBoundingBox().getCenter();
    }

    // 视角锁定逻辑（仅设置服务器视角）
    private void aimAtTarget(Entity target) {
        Vec3d targetPos = getTargetPosition(target);
        Vec3d playerEyes = mc.player.getEyePos();
        float[] rotations = calculateLookAngles(playerEyes, targetPos);

        // 更新服务器视角
        serverYaw = rotations[0];
        serverPitch = rotations[1];
    }

    // 计算视角角度
    private float[] calculateLookAngles(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distance));

        return new float[] {
                yaw % 360,
                Math.max(-90.0F, Math.min(90.0F, pitch))
        };
    }

    @Override
    public void onUpdate() {
        // 自由视角控制
        if (freeLook.getValue() && mc.player != null) {
            float speed = lookSpeed.getValueFloat();

            if (mc.options.rightKey.isPressed()) {
                clientYaw += speed;
            }
            if (mc.options.leftKey.isPressed()) {
                clientYaw -= speed;
            }
            if (mc.options.backKey.isPressed()) {
                clientPitch = Math.min(90, clientPitch + speed);
            }
            if (mc.options.forwardKey.isPressed()) {
                clientPitch = Math.max(-90, clientPitch - speed);
            }

            // 强制应用客户端视角 - 确保自由视角不受移动影响
            mc.player.setYaw(clientYaw);
            mc.player.setPitch(clientPitch);
        }
    }

    @Override
    public String getInfo() {
        return hasTarget ?
                String.format("%.1fm", currentTarget.getPos().distanceTo(mc.player.getPos())) : "Idle";
    }

    // 获取当前目标（供AutoShot使用）
    public Entity getCurrentTarget() {
        return currentTarget;
    }

    // 获取是否有有效目标
    public boolean hasTarget() {
        return hasTarget;
    }
}