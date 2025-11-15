package me.nullpoint.mod.modules.impl.client;

import me.nullpoint.api.events.eventbus.EventHandler;
import me.nullpoint.api.events.impl.UpdateWalkingEvent;
import me.nullpoint.api.utils.entity.EntityUtil;
import me.nullpoint.api.utils.entity.InventoryUtil;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.impl.combat.*;
import me.nullpoint.mod.modules.impl.player.SpeedMine;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Set;

public class ComboBreaks extends Module {
    public static ComboBreaks INSTANCE = new ComboBreaks();

    // 模块暂停控制
    private final Set<Module> pausedModules = new HashSet<>();
    private boolean wasUsingItem = false;
    // 新增：PopPotions选项
    public final BooleanSetting popPotions =
            add(new BooleanSetting("PopPotions", false));
    // 图腾触发检测状态
    private boolean totemTriggered = false;
    private boolean hadTotemLastTick = false;

    public ComboBreaks() {
        super("ComboBreaks", "切换操作优先级", Category.Client);
        INSTANCE = this;
    }

    @EventHandler
    public void onUpdate(UpdateWalkingEvent event) {
        if (!this.isOn()) return;

        boolean isUsingItem = mc.player != null && mc.player.isUsingItem();

        // 当玩家开始使用物品时暂停模块
        if (isUsingItem && !wasUsingItem) {
            pauseCombatModules();
        }
        // 当玩家停止使用物品时恢复模块
        else if (!isUsingItem && wasUsingItem) {
            resumeCombatModules();
        }

        wasUsingItem = isUsingItem;

        // 新增：图腾触发检测
        checkTotemTrigger();
        // 新增：触发后投掷药水
        if (totemTriggered) {
            popTurtlePotion();
            totemTriggered = false;
        }
    }

    // 新增：图腾触发检测方法
    private void checkTotemTrigger() {
        if (mc.player == null) return;

        // 检查当前是否持有图腾
        boolean hasTotemNow = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;

        // 如果上一刻有图腾但当前没有，则触发
        if (hadTotemLastTick && !hasTotemNow) {
            totemTriggered = true;
        }

        hadTotemLastTick = hasTotemNow;
    }

    // 新增：神龟药水投掷方法
    private void popTurtlePotion() {
        if (!popPotions.getValue()) return;
        if (mc.currentScreen != null) return;

        int turtleSlot = InventoryUtil.findItem(Items.POTION);
        if (turtleSlot != -1) {
            // 快捷栏中的药水
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(turtleSlot);
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, EntityUtil.getWorldActionId(mc.world)));
            InventoryUtil.switchToSlot(oldSlot);
        } else {
            // 背包中的药水
            int invSlot = InventoryUtil.findItemInventorySlot(Items.POTION);
            if (invSlot != -1) {
                int oldSlot = mc.player.getInventory().selectedSlot;
                InventoryUtil.inventorySwap(invSlot, oldSlot);
                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, EntityUtil.getWorldActionId(mc.world)));
                InventoryUtil.inventorySwap(invSlot, oldSlot);
                EntityUtil.syncInventory();
            }
        }
    }

    /**
     * 暂停所有指定的战斗模块
     */
    private void pauseCombatModules() {
        // 需要暂停的模块列表
        Module[] modulesToPause = {
                HoleFiller.INSTANCE,
                HoleKick.INSTANCE,
                AutoTrap.INSTANCE,
                AutoTrapPro.INSTANCE,
                AnchorAura.INSTANCE,
                AnchorAuraPro.INSTANCE,
                AutoCrystal.INSTANCE,
                AutoCrystalPro.INSTANCE,
                AutoCrystalMax.INSTANCE,
                WebAura.INSTANCE,
                WebAuraPro.INSTANCE,
                AutoClearHead.INSTANCE,
                AutoCity.INSTANCE,
                AutoCityDown.INSTANCE,
                AutoCityMax.INSTANCE,
        };

        for (Module module : modulesToPause) {
            if (module != null && module.isOn()) {
                module.disable();
                pausedModules.add(module);
            }
        }
    }

    /**
     * 恢复所有被暂停的战斗模块
     */
    private void resumeCombatModules() {
        for (Module module : pausedModules) {
            if (module != null && !module.isOn()) {
                module.enable();
            }
        }
        pausedModules.clear();
    }

    @Override
    public void onDisable() {
        // 当ComboBreaks被禁用时恢复所有被暂停的模块
        resumeCombatModules();
        wasUsingItem = false;
        totemTriggered = false;
        hadTotemLastTick = false;
    }
}