package net.minestom.server.extras.ac;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.utils.PacketUtils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class AntiCheat {
    private static final VarHandle ENABLED;

    static {
        try {
            ENABLED = MethodHandles.lookup().findStaticVarHandle(AntiCheat.class, "enabled", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private static boolean enabled;
    private static final Cache<Player, ClientContext> CONTEXT = Caffeine.newBuilder().weakKeys().build();

    public static void enable() {
        if (PacketUtils.VIEWABLE_PACKET)
            throw new IllegalStateException("Minestom anti-cheat is incompatible with the viewable packet feature.");
        if (!ENABLED.compareAndSet(false, true))
            MinecraftServer.LOGGER.warn("Minestom anti-cheat is already enabled.");

        var eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(PlayerPacketEvent.class, event -> {
            var context = CONTEXT.get(event.getPlayer(), player -> new ClientContext());
            context.queue.relaxedOffer(event.getPacket());
        });

        eventHandler.addListener(PlayerPacketOutEvent.class, event -> {
            var context = CONTEXT.get(event.getPlayer(), player -> new ClientContext());
            context.queue.relaxedOffer(event.getPacket());
        });

        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    CONTEXT.asMap().forEach((player, context) -> {
                        try {
                            context.process();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Ms-Watchdog");
        thread.setDaemon(true);
        thread.start();
    }
}
