package net.minestom.server.network.packet.client.play;

import net.minestom.server.crypto.ArgumentSignatures;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.NotNull;

public record ClientCommandChatPacket(@NotNull String message, long timestamp,
                                      long salt, @NotNull ArgumentSignatures signatures,
                                      boolean signed) implements ClientPacket {

    public ClientCommandChatPacket(BinaryReader reader) {
        this(reader.readSizedString(256), reader.readLong(),
                reader.readLong(), new ArgumentSignatures(reader), reader.readBoolean());
    }

    @Override
    public void write(@NotNull BinaryWriter writer) {
        writer.writeSizedString(message);
        writer.writeLong(timestamp);
        writer.writeLong(salt);
        writer.write(signatures);
        writer.writeBoolean(signed);
    }
}
