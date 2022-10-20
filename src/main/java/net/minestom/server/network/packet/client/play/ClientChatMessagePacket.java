package net.minestom.server.network.packet.client.play;

import net.minestom.server.crypto.LastSeenMessages;
import net.minestom.server.crypto.MessageSignature;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientChatMessagePacket(@NotNull String message,
                                      long timestamp, long salt,
                                      @Nullable MessageSignature signature,
                                      @NotNull LastSeenMessages.Update lastSeenMessages) implements ClientPacket {
    public ClientChatMessagePacket {
        if (message.length() > 256) {
            throw new IllegalArgumentException("Message cannot be more than 256 characters long.");
        }
    }

    public ClientChatMessagePacket(@NotNull NetworkBuffer reader) {
        this(reader.read(STRING),
                reader.read(LONG), reader.read(LONG),
                reader.readOptional(MessageSignature::new),
                new LastSeenMessages.Update(reader));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(STRING, message);
        writer.write(LONG, timestamp);
        writer.write(LONG, salt);
        writer.writeOptional(signature);
        writer.write(lastSeenMessages);
    }
}
