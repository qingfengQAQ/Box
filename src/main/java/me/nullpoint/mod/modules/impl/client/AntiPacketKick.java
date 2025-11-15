package me.nullpoint.mod.modules.impl.client;

import me.nullpoint.api.events.eventbus.EventHandler;
import me.nullpoint.api.events.impl.PacketEvent;
import me.nullpoint.api.utils.math.Timer;
import me.nullpoint.mod.modules.Module;
import me.nullpoint.mod.modules.settings.impl.BooleanSetting;
import me.nullpoint.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.Packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AntiPacketKick extends Module {
    // Settings
    public final SliderSetting packetLimit = add(new SliderSetting("Packets/s", 20, 5, 100, 1));
    public final BooleanSetting positionOnly = add(new BooleanSetting("Position Only", true));

    // Packet queue and timers
    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Timer packetTimer = new Timer();
    private int packetsSentThisSecond = 0;

    // Safe shutdown flag
    private volatile boolean isShuttingDown = false;

    public AntiPacketKick() {
        super("AntiPacketKick", Category.Client);
    }

    @Override
    public void onEnable() {
        isShuttingDown = false;
        packetTimer.reset();
        packetsSentThisSecond = 0;
    }

    @Override
    public void onDisable() {
        // Set safe shutdown flag
        isShuttingDown = true;

        // Safely clear queue: process remaining packets in main thread
        if (mc.player != null && mc.world != null) {
            while (!packetQueue.isEmpty()) {
                Packet<?> packet = packetQueue.poll();
                if (packet != null) {
                    mc.player.networkHandler.sendPacket(packet);
                }
            }
        } else {
            // If game environment is unavailable, clear queue directly
            packetQueue.clear();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        // Check if module is enabled using state field
        if (!state || mc.player == null || mc.world == null || isShuttingDown) return;

        // Check if it's a position packet
        boolean isPositionPacket = event.getPacket().getClass().getSimpleName().contains("Position");

        // If it's not a position packet and position-only mode is enabled, send directly
        if (positionOnly.isOpen() && !isPositionPacket) {
            packetsSentThisSecond++;
            return;
        }

        // Check if packet rate limit is reached
        if (packetsSentThisSecond < packetLimit.getValue()) {
            packetsSentThisSecond++;
            return;
        }

        // Add to queue for delayed sending
        event.setCancelled(true);
        packetQueue.offer((Packet) event.getPacket());
    }

    @Override
    public void onUpdate() {
        // Check if module is enabled using state field
        if (!state || mc.player == null || mc.world == null || isShuttingDown) return;

        // Reset counter every second
        if (packetTimer.passedMs(1000)) {
            packetsSentThisSecond = 0;
            packetTimer.reset();
        }

        // Process delayed packets - with safety limit
        int processed = 0;
        int maxPerTick = 50; // Prevent processing too many packets in one tick

        while (!packetQueue.isEmpty() &&
                packetsSentThisSecond < packetLimit.getValue() &&
                processed < maxPerTick) {

            Packet<?> packet = packetQueue.poll();
            if (packet != null) {
                mc.player.networkHandler.sendPacket(packet);
                packetsSentThisSecond++;
                processed++;
            }
        }
    }

    @Override
    public String getInfo() {
        return "Limit: " + (int)packetLimit.getValue();
    }
}