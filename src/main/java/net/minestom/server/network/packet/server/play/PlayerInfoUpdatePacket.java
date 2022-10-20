package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.GameMode;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.binary.BinaryWriter;
import net.minestom.server.utils.binary.Writeable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public record PlayerInfoUpdatePacket(@NotNull EnumSet<@NotNull Action> actions,
                                     @NotNull List<@NotNull Entry> entries) implements ServerPacket {
    public PlayerInfoUpdatePacket {
        actions = EnumSet.copyOf(actions);
        entries = List.copyOf(entries);
    }

    @Override
    public void write(@NotNull BinaryWriter writer) {
        writer.writeEnumSet(actions, Action.class);
        writer.writeVarIntList(entries, (binaryWriter, entry) -> {
            binaryWriter.writeUuid(entry.uuid);
            for (Action action : actions) {
                action.writer.write(binaryWriter, entry);
            }
        });
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.PLAYER_INFO_UPDATE;
    }

    public record Entry(UUID uuid, String username, List<Property> properties,
                        boolean listed, int latency, GameMode gameMode,
                        @Nullable Component displayName) {
        public Entry {
            properties = List.copyOf(properties);
        }
    }

    public record Property(@NotNull String name, @NotNull String value,
                           @Nullable String signature) implements Writeable {
        public Property(String name, String value) {
            this(name, value, null);
        }

        public Property(BinaryReader reader) {
            this(reader.readSizedString(), reader.readSizedString(),
                    reader.readBoolean() ? reader.readSizedString() : null);
        }

        @Override
        public void write(BinaryWriter writer) {
            writer.writeSizedString(name);
            writer.writeSizedString(value);
            writer.writeBoolean(signature != null);
            if (signature != null) writer.writeSizedString(signature);
        }
    }

    public enum Action {
        ADD_PLAYER((writer, entry) -> {
            writer.writeSizedString(entry.username);
            writer.writeVarIntList(entry.properties, BinaryWriter::write);
        }),
        INITIALIZE_CHAT(null),
        UPDATE_GAME_MODE((writer, entry) -> writer.writeVarInt(entry.gameMode.ordinal())),
        UPDATE_LISTED((writer, entry) -> writer.writeBoolean(entry.listed)),
        UPDATE_LATENCY((writer, entry) -> writer.writeVarInt(entry.latency)),
        UPDATE_DISPLAY_NAME((writer, entry) -> {
            writer.writeBoolean(entry.displayName != null);
            if (entry.displayName != null) writer.writeComponent(entry.displayName);
        });

        final Writer writer;

        Action(Writer writer) {
            this.writer = writer;
        }

        interface Writer {
            void write(BinaryWriter writer, Entry entry);
        }
    }
}
