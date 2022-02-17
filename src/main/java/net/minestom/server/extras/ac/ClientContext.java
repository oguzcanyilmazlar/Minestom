package net.minestom.server.extras.ac;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionAndRotationPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.client.play.ClientSettingsPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jctools.queues.MpscUnboundedXaddArrayQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

final class ClientContext {
    final MpscUnboundedXaddArrayQueue<Object> queue = new MpscUnboundedXaddArrayQueue<>(1024);

    private final Map<Class<?>, Consumer<ClientPacket>> clientHandler = new HashMap<>();
    private final Map<Class<?>, Consumer<ServerPacket>> serverHandler = new HashMap<>();

    private Pos position;

    private ClientSettingsPacket lastSettings;
    private final Long2ObjectMap<Chunk> visibleChunks = new Long2ObjectOpenHashMap<>();

    public ClientContext() {
        registerClientPacket(ClientSettingsPacket.class, this::handleClientSettings);
        registerClientPacket(ClientPlayerPositionAndRotationPacket.class, packet -> handleMovement(packet.position()));
        registerClientPacket(ClientPlayerPositionPacket.class, packet -> handleMovement(packet.position()));

        registerServerPacket(ChunkDataPacket.class, this::handleChunkData);
        registerServerPacket(UnloadChunkPacket.class, this::handleUnloadChunk);
    }

    synchronized void process() {
        this.queue.drain(e -> {
            if (e instanceof ClientPacket) {
                handleClient((ClientPacket) e);
            } else if (e instanceof ServerPacket) {
                handleServer((ServerPacket) e);
            } else {
                throw new IllegalArgumentException("Unknown packet type: " + e.getClass().getSimpleName());
            }
        });
    }

    private void handleClient(ClientPacket packet) {
        var handler = clientHandler.get(packet.getClass());
        if (handler != null) {
            handler.accept(packet);
        } else {
            System.out.println("client " + packet.getClass().getSimpleName());
        }
    }

    private void handleClientSettings(ClientSettingsPacket settings) {
        this.lastSettings = settings;
    }

    private void handleMovement(Point position) {
        this.position = position instanceof Pos pos ? pos : this.position.withCoord(position);
    }

    private void handleServer(ServerPacket packet) {
        var handler = serverHandler.get(packet.getClass());
        if (handler != null) {
            handler.accept(packet);
        } else {
            System.out.println("server " + packet.getClass().getSimpleName());
        }
    }

    private void handleChunkData(ChunkDataPacket chunkDataPacket) {
        final long index = ChunkUtils.getChunkIndex(chunkDataPacket.chunkX(), chunkDataPacket.chunkZ());
        this.visibleChunks.put(index, new Chunk(chunkDataPacket));
    }

    private void handleUnloadChunk(UnloadChunkPacket unloadChunkPacket) {
        final long index = ChunkUtils.getChunkIndex(unloadChunkPacket.chunkX(), unloadChunkPacket.chunkZ());
        this.visibleChunks.remove(index);
    }

    private <T> void registerClientPacket(Class<T> packetClass, Consumer<T> consumer) {
        this.clientHandler.put(packetClass, (Consumer<ClientPacket>) consumer);
    }

    private <T> void registerServerPacket(Class<T> packetClass, Consumer<T> consumer) {
        this.serverHandler.put(packetClass, (Consumer<ServerPacket>) consumer);
    }

    final class Chunk {
        int x, z;

        Chunk(ChunkDataPacket packet) {
            var chunk = packet.chunkData();
            var chunkData = chunk.data();
            var reader = new BinaryReader(chunkData);
        }
    }
}
