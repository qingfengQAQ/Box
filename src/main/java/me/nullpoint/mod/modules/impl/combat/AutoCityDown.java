package me.nullpoint.mod.modules.impl.combat;

import me.nullpoint.api.events.impl.UpdateWalkingEvent;
import me.nullpoint.api.utils.combat.CombatUtil;
import me.nullpoint.api.utils.entity.EntityUtil;
import me.nullpoint.api.utils.world.BlockPosX;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.impl.client.ComboBreaks;
import me.nullpoint.mod.modules.impl.player.SpeedMine;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.settings.impl.EnumSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class AutoCityDown extends Module {
    public static AutoCityDown INSTANCE;
    private final EnumSetting<AutoCity.Page> page =
            add(new EnumSetting<>("Page", AutoCity.Page.Main));
    private final BooleanSetting pro = add(new BooleanSetting("Pro", true));
    public final SliderSetting targetRange =
            add(new SliderSetting("TargetRange", 5.0, 0.0, 8.0, 0.1));
    BooleanSetting eatingPause =
            add(new BooleanSetting("EatingPause", true, v -> page.getValue() == AutoCity.Page.Main));

    public AutoCityDown() {
        super("AutoCityDown", Category.Combat);
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        if (eatingPause.getValue() && EntityUtil.isUsing()) return;
        PlayerEntity player = CombatUtil.getClosestEnemy(targetRange.getValue());
        if (player == null) return;
        doBreak(player);
    }

    private void doBreak(PlayerEntity player) {
        BlockPos playerPos = EntityUtil.getEntityPos(player, true);

        if (pro.getValue()) {
            // 无论任何情况，优先挖掘玩家脚下 -1 格的方块
            BlockPos downPos = new BlockPosX(player.getX(), player.getY() - 1, player.getZ());
            SpeedMine.INSTANCE.mine(downPos);
            return; // Pro 模式下只挖掘玩家下方的一格方块
        }

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
    public enum Page {
        Main
    }
}