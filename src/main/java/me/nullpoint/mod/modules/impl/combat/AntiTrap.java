package me.nullpoint.mod.modules.impl.combat;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.impl.player.SpeedMine;

public class AntiTrap extends Module {
    private final BooleanSetting front = add(new BooleanSetting("Front", true));
    private final BooleanSetting right = add(new BooleanSetting("Right", true));
    private final BooleanSetting left = add(new BooleanSetting("Left", true));
    private final BooleanSetting back = add(new BooleanSetting("Back", true));

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public AntiTrap() {
        super("AntiTrap", Category.Combat);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        // 获取玩家头部坐标（下半身坐标 +1）
        BlockPos headPos = mc.player.getBlockPos().up();

        // 获取玩家水平方向
        Direction facing = getHorizontalFacing();

        // 按优先级顺序检查四个方向
        checkAndBreak(headPos, facing, this.front.getValue());            // 前
        checkAndBreak(headPos, facing.rotateYClockwise(), right.getValue());   // 右
        checkAndBreak(headPos, facing.rotateYCounterclockwise(), left.getValue()); // 左
        checkAndBreak(headPos, facing.getOpposite(), back.getValue());        // 后
    }

    private Direction getHorizontalFacing() {
        // 将yaw角度转换为8方向后取最近的4个主方向
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
        return Direction.fromRotation(yaw);
    }

    private void checkAndBreak(BlockPos headPos, Direction dir, boolean enabled) {
        if (!enabled) return;

        BlockPos targetPos = headPos.offset(dir);
        BlockState state = mc.world.getBlockState(targetPos);

        // 检测方块是否可破坏（非空气且硬度>=0）
        if (state.isAir() || state.getHardness(mc.world, targetPos) < 0) return;

        // 发送开始破坏包
        SpeedMine.INSTANCE.mine(headPos);
        SpeedMine.INSTANCE.mine(targetPos);



    }
}