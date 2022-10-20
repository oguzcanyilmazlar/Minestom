package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record PlayerInfoRemovePacket(@NotNull List<@NotNull UUID> uuids) implements ServerPacket {
    public PlayerInfoRemovePacket {
        uuids = List.copyOf(uuids);
    }

    public PlayerInfoRemovePacket(@NotNull BinaryReader reader) {
        this(reader.readVarIntList(BinaryReader::readUuid));
    }

    @Override
    public void write(BinaryWriter writer) {
        writer.writeVarInt(uuids.size());
        for (UUID uuid : uuids) {
            writer.writeUuid(uuid);
        }
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.PLAYER_INFO_REMOVE;
    }
}
