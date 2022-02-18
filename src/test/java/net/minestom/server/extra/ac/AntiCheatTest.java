package net.minestom.server.extra.ac;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class AntiCheatTest extends UtilsAC {

    @Test
    public void test() throws URISyntaxException, IOException {
        retrieveReplay("replay_demo.mcpr");
    }
}
