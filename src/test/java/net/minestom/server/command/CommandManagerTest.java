package net.minestom.server.command;

import net.minestom.server.command.builder.Command;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandManagerTest {

    @Test
    public void testCommandRegistration() {
        var manager = new CommandManager();

        var command = new Command("name1", "name2");

        manager.register(command);

        assertTrue(manager.commandExists("name1"));
        assertTrue(manager.commandExists("name2"));
        assertFalse(manager.commandExists("name3"));

        manager.unregister(command);

        assertFalse(manager.commandExists("name1"));
        assertFalse(manager.commandExists("name2"));
        assertFalse(manager.commandExists("name3"));
    }

    @Test
    public void testUnknownCommandCallback() {
        var manager = new CommandManager();

        AtomicBoolean check = new AtomicBoolean(false);
        manager.setUnknownCommandCallback((sender, command) -> check.set(true));

        manager.register(new Command("valid_command"));

        manager.executeServerCommand("valid_command");
        assertFalse(check.get());

        manager.executeServerCommand("invalid_command");
        assertTrue(check.get());
    }
}
