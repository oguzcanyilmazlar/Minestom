package net.minestom.demo.commands;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

import static net.minestom.server.command.builder.arguments.ArgumentType.*;

public class PathfindCommand extends Command {

    public PathfindCommand() {
        super("pathfind");

        addSyntax(
                this::usageE2E,
                Entity("from"),
                Entity("to")
        );
    }

    private void usageE2E(CommandSender sender, CommandContext context) {
        EntityFinder from = context.get("from");
        EntityFinder to = context.get("to");

        List<Entity> fromList = from.find(sender);
        Entity toEntity = to.findFirstEntity(sender);

        if (toEntity == null) {
            sender.sendMessage("No entity found");
            return;
        }

        Pos destination = toEntity.getPosition();
        destination = destination.add(toEntity.getBoundingBox().relativeStart());

        for (Entity fromEntity : fromList) {
            if (fromEntity instanceof EntityCreature creature) {
                sender.sendMessage("Pathfinding from " + fromEntity + " to " + toEntity);
                creature.getNavigator().setPathTo(destination);
            }
        }
    }
}