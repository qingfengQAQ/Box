package me.nullpoint.mod.modules.impl.client;

import me.nullpoint.api.utils.render.Render2DUtil;
import me.nullpoint.api.utils.math.FadeUtils;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.settings.impl.ColorSetting;
import me.nullpoint.mod.modules.settings.impl.EnumSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import me.nullpoint.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Notify extends Module {
    // 使用线程安全的CopyOnWriteArrayList替代ArrayList
    public static final CopyOnWriteArrayList<Notifys> notifyList = new CopyOnWriteArrayList<>();
    public static Notify INSTANCE;
    public final EnumSetting<Notifys.type> type = add(new EnumSetting<>("Type", Notifys.type.Both));
    public final EnumSetting<Notifys.mode> mode = add(new EnumSetting<>("Type", Notifys.mode.Fill));
    private final SliderSetting notifyY = add(new SliderSetting("Y", 18, -50, 500));
    private final ColorSetting fillcolor = add(new ColorSetting("FillColor",new Color(20, 20, 20, 100)));
    private final ColorSetting linecolor = add(new ColorSetting("LineColor",new Color(140,140,250,225)));
    public final SliderSetting notifyX =
            add(new SliderSetting("notifyX", 256, 18, 500));

    public Notify(){
        super("Notify","Notify Test",Category.Client);
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        boolean bl = true;
        int n = (int) (379 - this.notifyY.getValue());
        int n2 = notifyX.getValueInt() + 500;

        // 创建临时列表来避免并发修改
        List<Notifys> toRemove = new ArrayList<>();

        // 使用迭代器安全地遍历列表
        Iterator<Notifys> iterator = notifyList.iterator();
        while (iterator.hasNext()) {
            Notifys notifys = iterator.next();

            // 添加空值检查，防止NullPointerException
            if (notifys == null || notifys.first == null || notifys.firstFade == null) {
                toRemove.add(notifys);
                continue;
            }

            if (notifys.delayed < 1) {
                toRemove.add(notifys);
                continue;
            }

            bl = false;

            // 检查动画状态
            if (notifys.delayed < 5 && !notifys.end) {
                notifys.end = true;
                if (notifys.endFade != null) {
                    notifys.endFade.reset();
                }
            }

            // 检查yFade是否为空
            if (notifys.yFade != null) {
                n = (int)((double)n - 18.0 * notifys.yFade.easeOutQuad());
            }

            String string = notifys.first;

            // 检查firstFade和endFade是否为空
            double d;
            if (notifys.delayed < 5) {
                if (notifys.endFade != null) {
                    d = (double)n2 - (double)(mc.textRenderer.getWidth(string) + 10) * (1.0 - notifys.endFade.easeOutQuad());
                } else {
                    d = n2;
                }
            } else {
                if (notifys.firstFade != null) {
                    d = (double)n2 - (double)(mc.textRenderer.getWidth(string) + 10) * notifys.firstFade.easeOutQuad();
                } else {
                    d = n2;
                }
            }

            // 渲染通知背景
            Render2DUtil.drawRound(drawContext.getMatrices(),(int)d, n, 10 + mc.textRenderer.getWidth(string), 15,4f, fillcolor.getValue());

            // 渲染通知文本
            drawContext.drawText(mc.textRenderer, string, 5 + (int)d, 4 + n , new Color(255,255,255,255).getRGB(), true);

            // 处理结束动画
            if (notifys.delayed < 5) {
                if (notifys.yFade != null && notifys.endFade != null) {
                    n = (int)((double)n + 18.0 * notifys.yFade.easeOutQuad() - 18.0 * (1.0 - notifys.endFade.easeOutQuad()));
                }
                continue;
            }

            // 渲染进度条
            if (notifys.delayed > 4) {
                Render2DUtil.drawRect(drawContext.getMatrices(),(int)d+2f, n + 14, (float) ((10 + mc.textRenderer.getWidth(string)) * (notifys.delayed - 4)-2) / 62, 1, linecolor.getValue());
            }
        }

        // 安全地移除过期的通知
        if (!toRemove.isEmpty()) {
            notifyList.removeAll(toRemove);
        }

        if (bl) {
            notifyList.clear();
        }
    }

    @Override
    public void onUpdate() {
        if (UIModule.INSTANCE != null && UIModule.INSTANCE.state){
            return;
        }

        // 创建临时列表来避免并发修改
        List<Notifys> toRemove = new ArrayList<>();

        // 安全地遍历和更新通知
        Iterator<Notifys> iterator = notifyList.iterator();
        while (iterator.hasNext()) {
            Notifys notifys = iterator.next();

            // 添加空值检查
            if (notifys == null || notifys.first == null || notifys.firstFade == null) {
                toRemove.add(notifys);
                continue;
            }

            --notifys.delayed;

            // 如果通知已过期，标记为需要移除
            if (notifys.delayed <= 0) {
                toRemove.add(notifys);
            }
        }

        // 安全地移除过期的通知
        if (!toRemove.isEmpty()) {
            notifyList.removeAll(toRemove);
        }
    }

    @Override
    public void onDisable() {
        notifyList.clear();
    }

    // 添加通知的静态方法，确保线程安全
    public static void addNotification(String message) {
        if (message != null && !message.isEmpty()) {
            notifyList.add(new Notifys(message));
        }
    }

    public static class Notifys {
        public final FadeUtils firstFade;
        public final FadeUtils endFade;
        public final FadeUtils yFade;
        public final String first;
        public int delayed = 55;
        public boolean end;

        public Notifys(String string) {
            this.firstFade = new FadeUtils(500L);
            this.endFade = new FadeUtils(350L);
            this.yFade = new FadeUtils(500L);
            this.first = string;

            // 安全地初始化FadeUtils对象
            if (this.firstFade != null) {
                this.firstFade.reset();
            }
            if (this.yFade != null) {
                this.yFade.reset();
            }
            if (this.endFade != null) {
                this.endFade.reset();
            }

            this.end = false;
        }

        public enum type {
            Notify,
            Chat,
            Both
        }

        public enum mode {
            Line,
            Fill
        }
    }
}