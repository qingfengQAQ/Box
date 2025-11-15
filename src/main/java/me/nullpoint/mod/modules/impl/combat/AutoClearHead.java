package me.nullpoint.mod.modules.impl.combat;

import me.nullpoint.api.events.impl.UpdateWalkingEvent;
import me.nullpoint.api.utils.world.BlockUtil;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.impl.client.ComboBreaks;
import me.nullpoint.mod.modules.impl.player.SpeedMine;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class AutoClearHead extends Module {
    public static AutoClearHead INSTANCE;
    private final SliderSetting range = add(new SliderSetting("Range", 3.0, 0.0, 5.0, 1.0));

    // 新增 VClipBlock 选项
    private final BooleanSetting vclipBlock = add(new BooleanSetting("VClipBlock", false));
    // 新增头部蜘蛛网清除选项
    private final BooleanSetting clearHeadWeb = add(new BooleanSetting("ClearHeadWeb", false));
    // 新增腿部蜘蛛网清除选项
    private final BooleanSetting clearLegWeb = add(new BooleanSetting("ClearLegWeb", false));

    public AutoClearHead() {
        super("AutoClearHead", Category.Combat);
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        PlayerEntity player = mc.player;
        if (player == null) return;

        double rangeValue = range.getValue();
        BlockPos feetPos = new BlockPos(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ())
        );

        // 新增：清除头部蜘蛛网逻辑
        if (clearHeadWeb.getValue()) {
            BlockPos headPos = feetPos.up(1);
            if (isWeb(headPos)) {
                SpeedMine.INSTANCE.mine(headPos);
                return;
            }
        }

        // 新增：清除腿部蜘蛛网逻辑
        if (clearLegWeb.getValue()) {
            if (isWeb(feetPos)) {
                SpeedMine.INSTANCE.mine(feetPos);
                return;
            }
        }

        if (rangeValue == 0) return; // 如果范围设置为0，不进行任何挖掘

        // 获取玩家高度
        double playerHeight = player.getHeight();

        for (int i = 1; i <= rangeValue; i++) {
            // 获取玩家头顶上方 i 格的方块位置
            BlockPos headPos = feetPos.up((int) (playerHeight + i));

            // 新增 VClipBlock 逻辑
            if (vclipBlock.getValue() && i == 1) {
                continue; // 跳过头上1格的方块
            }

            // 检查头顶上方 i 格的方块
            Block blockAboveHead = BlockUtil.getBlock(headPos);
            if (blockAboveHead != null && blockAboveHead != Blocks.AIR) {
                // 挖掘头顶上方 i 格的方块
                SpeedMine.INSTANCE.mine(headPos);
                return;// 挖掘完当前方块后，停止当前更新周期
            }
        }
    }

    // 新增：判断指定位置是否为蜘蛛网
    private boolean isWeb(BlockPos pos) {
        Block block = BlockUtil.getBlock(pos);
        return block != null && block == Blocks.COBWEB;
    }

    private boolean playerIsPerformingAction() {
        // 检测玩家是否在操作（移动、攻击、使用物品等）
        return mc.options.useKey.isPressed();
    }

    public void onUpdate(UpdateWalkingEvent event) {
        if (ComboBreaks.INSTANCE.isOn() && playerIsPerformingAction()) {
            // 如果ComboBreaks开启且玩家正在操作，则跳过模块功能
        }
    }

    @Override
    public String getInfo() {
        if (vclipBlock.getValue()) return "VClip";
        if (clearHeadWeb.getValue()) return "HeadWeb";
        if (clearLegWeb.getValue()) return "LegWeb";
        return null;
    }
}