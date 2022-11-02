package net.minestom.server.network.packet.client.play;

import net.minestom.server.entity.player.ChatSession;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

public record ClientChatSessionUpdatePacket(ChatSession chatSession) implements ClientPacket {
    public ClientChatSessionUpdatePacket(@NotNull NetworkBuffer reader) {
        this(new ChatSession(reader));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(chatSession);
    }
}
