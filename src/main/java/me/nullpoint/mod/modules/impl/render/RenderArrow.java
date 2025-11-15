package me.nullpoint.mod.modules.impl.render;

import me.nullpoint.api.utils.entity.EntityUtil;
import me.nullpoint.api.utils.math.FadeUtils;
import me.nullpoint.api.utils.render.Render3DUtil;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.settings.impl.ColorSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderArrow extends Module {
    // 轨迹设置
    public final SliderSetting duration =
            add(new SliderSetting("Duration(ms)", 2000, 500, 5000, 100));
    public final SliderSetting fadeInTime =
            add(new SliderSetting("Fade In(ms)", 300, 0, 1000, 50));
    public final SliderSetting fadeOutTime =
            add(new SliderSetting("Fade Out(ms)", 500, 0, 2000, 50));

    // 颜色设置
    public final ColorSetting ownColor =
            add(new ColorSetting("Own Trajectory", new Color(0, 255, 0, 255)));
    public final ColorSetting otherColor =
            add(new ColorSetting("Other Trajectory", new Color(255, 0, 0, 255)));

    // 轨迹数据存储
    private final Map<UUID, TrajectoryData> trajectoryMap = new HashMap<>();

    public RenderArrow() {
        super("RenderArrow", "Renders arrow and pearl trajectories", Category.Render);
    }

    @Override
    public void onEnable() {
        trajectoryMap.clear();
    }

    @Override
    public void onDisable() {
        trajectoryMap.clear();
    }

    public void onUpdate() {
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        long durationMs = (long) duration.getValue();

        // 清理过期轨迹
        trajectoryMap.entrySet().removeIf(entry ->
                currentTime - entry.getValue().startTime > durationMs);

        // 查找新的箭和末影珍珠
        for (Entity entity : mc.world.getEntities()) {
            if (isTrackableEntity(entity)) {
                UUID entityId = entity.getUuid();

                // 如果实体不在轨迹图中，添加新轨迹
                if (!trajectoryMap.containsKey(entityId)) {
                    boolean isOwn = false;
                    if (entity instanceof ProjectileEntity) {
                        Entity owner = ((ProjectileEntity) entity).getOwner();
                        if (owner != null) {
                            isOwn = owner.getUuid().equals(mc.player.getUuid());
                        }
                    }

                    trajectoryMap.put(entityId, new TrajectoryData(
                            entity,
                            System.currentTimeMillis(),
                            isOwn
                    ));
                }

                // 更新现有轨迹
                TrajectoryData data = trajectoryMap.get(entityId);
                if (data != null) {
                    data.update(entity);
                }
            }
        }
    }

    public void onRender3D() {
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        long fadeInMs = (long) fadeInTime.getValue();
        long fadeOutMs = (long) fadeOutTime.getValue();
        long durationMs = (long) duration.getValue();

        // 渲染所有轨迹
        for (TrajectoryData data : trajectoryMap.values()) {
            if (data.points.size() < 2) continue;

            // 计算整体透明度
            long elapsed = currentTime - data.startTime;
            float overallAlpha = calculateAlpha(elapsed, fadeInMs, fadeOutMs, durationMs);

            // 跳过完全透明的轨迹
            if (overallAlpha <= 0.01f) continue;

            // 获取轨迹颜色
            Color baseColor = data.isOwn ? ownColor.getValue() : otherColor.getValue();
            Color trajectoryColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    (int)(baseColor.getAlpha() * overallAlpha)
            );

            // 渲染轨迹
            renderTrajectory(data.points, trajectoryColor);
        }
    }

    // 检查实体是否可跟踪
    private boolean isTrackableEntity(Entity entity) {
        return (entity instanceof ArrowEntity || entity instanceof EnderPearlEntity) &&
                !entity.isRemoved() &&
                entity.age < 100; // 只跟踪新生成的实体
    }

    // 计算轨迹点透明度
    private float calculateAlpha(long elapsed, long fadeInMs, long fadeOutMs, long durationMs) {
        // 淡入阶段
        if (elapsed < fadeInMs && fadeInMs > 0) {
            return Math.min(1.0f, (float) elapsed / fadeInMs);
        }

        // 淡出阶段
        long fadeOutStart = durationMs - fadeOutMs;
        if (elapsed > fadeOutStart && fadeOutMs > 0) {
            return 1.0f - Math.min(1.0f, (float)(elapsed - fadeOutStart) / fadeOutMs);
        }

        // 完全可见阶段
        return 1.0f;
    }

    // 渲染轨迹线 (修复后的版本)
    private void renderTrajectory(ArrayList<Vec3d> points, Color color) {
        // 渲染实体轨迹线
        for (int i = 1; i < points.size(); i++) {
            Vec3d prev = points.get(i - 1);
            Vec3d current = points.get(i);

            Render3DUtil.drawLine(
                    (float) prev.x, (float) prev.y, (float) prev.z,
                    (float) current.x, (float) current.y, (float) current.z,
                    color, 2.0f
            );
        }

        // 渲染预测线（仅当有足够点时）
        if (points.size() >= 2) {
            Vec3d lastPoint = points.get(points.size() - 1);
            Vec3d secondLast = points.get(points.size() - 2);
            Vec3d direction = lastPoint.subtract(secondLast).normalize();

            // 创建半透明颜色用于预测线
            Color predictedColor = new Color(
                    color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 2
            );

            // 绘制预测轨迹
            Vec3d start = lastPoint;
            for (int i = 1; i <= 5; i++) {
                Vec3d end = start.add(direction.multiply(0.5));

                Render3DUtil.drawLine(
                        (float) start.x, (float) start.y, (float) start.z,
                        (float) end.x, (float) end.y, (float) end.z,
                        predictedColor, 1.0f
                );

                start = end; // 下一段的起点是当前段的终点
            }
        }
    }

    // 轨迹数据存储类
    private static class TrajectoryData {
        public final long startTime;
        public final ArrayList<Vec3d> points = new ArrayList<>();
        public final boolean isOwn;

        public TrajectoryData(Entity entity, long startTime, boolean isOwn) {
            this.startTime = startTime;
            this.isOwn = isOwn;
            update(entity);
        }

        public void update(Entity entity) {
            Vec3d pos = entity.getPos();

            // 添加新点（如果位置有显著变化）
            if (points.isEmpty() ||
                    points.get(points.size() - 1).squaredDistanceTo(pos) > 0.01) {
                points.add(pos);
            }

            // 限制轨迹点数量
            if (points.size() > 50) {
                points.remove(0);
            }
        }
    }
}