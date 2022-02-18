package net.minestom.server.extra.ac;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.studio.ReplayStudio;
import net.minestom.server.MinecraftServer;

import java.io.File;

public class UtilsAC {

    private static final ReplayStudio STUDIO = new ReplayStudio();

    void retrieveReplay(String name) {
        try {
            final File dir = new File(ClassLoader.getSystemResource(name).toURI());
            final ReplayFile replayFile = new ZipReplayFile(STUDIO, dir);

            final PacketTypeRegistry registry = PacketTypeRegistry.get(ProtocolVersion.getProtocol(MinecraftServer.PROTOCOL_VERSION), State.PLAY);
            PacketStream stream = replayFile.getPacketData(registry).asPacketStream();
            stream.start();
            stream.addFilter(new ProgressFilter()); // Required for some reason

            int count = 0;
            PacketData packetData;
            while ((packetData = stream.next()) != null) {
                // TODO how to retrieve server-bound packets?
                System.out.println("packet " + packetData.getPacket().getType() + " - " + packetData.getTime());
                if (count++ >= 10) break;
            }

            stream.end();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class ProgressFilter implements StreamFilter {
        @Override
        public String getName() {
            return "progress";
        }

        @Override
        public void init(Studio studio, JsonObject config) {
        }

        @Override
        public void onStart(PacketStream stream) {
        }

        @Override
        public boolean onPacket(PacketStream stream, PacketData data) {
            return true;
        }

        @Override
        public void onEnd(PacketStream stream, long timestamp) {
        }
    }
}
