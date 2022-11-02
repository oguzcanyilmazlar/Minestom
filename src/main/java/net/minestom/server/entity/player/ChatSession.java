package net.minestom.server.entity.player;

import net.minestom.server.crypto.PlayerPublicKey;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ChatSession(UUID sessionId, PlayerPublicKey publicKey) implements NetworkBuffer.Writer {
    public ChatSession(NetworkBuffer reader) {
        this(reader.read(NetworkBuffer.UUID), new PlayerPublicKey(reader));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, sessionId);
        writer.write(publicKey);
    }
}
