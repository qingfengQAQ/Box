package me.nullpoint.api.utils.math;

import net.minecraft.util.math.Vec3d;

public class MathUtilPos {
    // 计算从起点到终点的视角角度
    public static float[] calculateLookAngles(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distance));

        return new float[] { yaw % 360, pitch };
    }

    // 计算两个旋转角度的差值
    public static double angleBetween(float[] rot1, float[] rot2) {
        double yawDiff = Math.abs(rot1[0] - rot2[0]);
        double pitchDiff = Math.abs(rot1[1] - rot2[1]);
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }
}