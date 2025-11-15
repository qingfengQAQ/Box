package me.nullpoint.mod.modules.impl.movement;

import me.nullpoint.api.events.eventbus.EventHandler;
import me.nullpoint.api.managers.CommandManager;
import me.nullpoint.api.utils.world.BlockUtil;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.awt.event.KeyEvent;

public class PullUp extends Module {
    // 添加Range滑动进度条
    private final SliderSetting range =
            add(new SliderSetting("Range", 3.0, 3.0, 3.0, 0.1));

    // 检测范围设置
    private final BooleanSetting checkHeadBlocks =
            add(new BooleanSetting("Check Head Blocks", true));

    // 消息通知设置
    private final BooleanSetting showMessage =
            add(new BooleanSetting("Show Message", true));

    public PullUp() {
        super("PullUp", "静默垂直传送", Category.Movement);
    }

    @Override
    public void onEnable() {
        // 当模块启用时执行传送
        executeVClip();
    }

    /**
     * 执行静默垂直传送
     */
    private void executeVClip() {
        // 检查头部方块条件
        if (checkHeadBlocks.getValue() && !canVClip()) {
            // 显示错误消息
            if (showMessage.getValue()) {
                CommandManager.sendChatMessage("§c[!] Unable to execute PullUp.");
                CommandManager.sendChatMessage("§e[?] 1 block on the head, 2 and 3 no blocks");
            }
            disable();
            return;
        }

        // 构建命令前缀（使用用户设定的Range值）
        String commandPrefix = ";vclip " + range.getValue();

        // 执行静默传送命令
        mc.player.networkHandler.sendChatMessage(commandPrefix);

        // 显示成功消息
        if (showMessage.getValue()) {
            CommandManager.sendChatMessage("§a [√] PullUp executed with range: " + range.getValue());
        }
    }

    /**
     * 检查是否满足VClip条件
     * @return 是否满足条件
     */
    private boolean canVClip() {
        if (mc.player == null) return false;

        // 获取玩家脚部位置
        BlockPos feetPos = new BlockPos(
                (int) Math.floor(mc.player.getX()),
                (int) Math.floor(mc.player.getY()),
                (int) Math.floor(mc.player.getZ())
        );

        // 检查头上1格方块
        BlockPos headPos1 = feetPos.up(2);
        Block block1 = BlockUtil.getBlock(headPos1);

        // 检查头上2格方块
        BlockPos headPos2 = feetPos.up(3);
        Block block2 = BlockUtil.getBlock(headPos2);

        // 检查头上3格方块
        BlockPos headPos3 = feetPos.up(4);
        Block block3 = BlockUtil.getBlock(headPos3);

        // 条件1: 头上1格必须有方块（非空气）
        boolean condition1 = block1 != null && block1 != Blocks.AIR;

        // 条件2: 头上2格必须没有方块（空气）
        boolean condition2 = block2 == null || block2 == Blocks.AIR;

        // 条件3: 头上3格必须没有方块（空气）
        boolean condition3 = block3 == null || block3 == Blocks.AIR;

        return condition1 && condition2 && condition3;
    }

    @Override
    public String getInfo() {
        return "UP";
    }
}