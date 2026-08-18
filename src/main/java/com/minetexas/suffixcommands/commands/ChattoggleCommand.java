package com.minetexas.suffixcommands.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCSettings;

public class ChattoggleCommand extends CommandBase {

    @Override
    public void init() {
        command = "/chattoggle";
        displayName = "Toggle chat spying";
        // no help entries added — this command intentionally has no help output
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        init();
        this.args = args;
        this.sender = sender;

        // Only players can toggle spy
        if (!(sender instanceof Player)) {
            sendError(sender, "You must be a player to toggle spy.");
            return true;
        }
        Player player = (Player) sender;
        // Allow OPs by default; otherwise require permission
        if (!player.isOp() && !player.hasPermission("suffixcommands.chat.spy")) {
            return false;
        }

        if (args.length == 0) {
            sendMessage(sender, SCColor.Yellow + "Usage: /chattoggle spy (toggles spy mode)");
            return true;
        }

        if (args[0].equalsIgnoreCase("spy")) {
            // double-check permissions at toggle time
            if (!player.isOp() && !player.hasPermission("suffixcommands.chat.spy")) {
                sendError(sender, "You don't have permission to toggle spy mode.");
                return true;
            }
            String uuid = player.getUniqueId().toString();
            if (SCSettings.spyToggles.contains(uuid)) {
                SCSettings.spyToggles.remove(uuid);
                sendMessage(sender, SCColor.Green + "Badge chat spy DISABLED.");
            } else {
                SCSettings.spyToggles.add(uuid);
                sendMessage(sender, SCColor.Green + "Badge chat spy ENABLED.");
            }
            return true;
        }

        sendError(sender, "Unknown subcommand");
        return true;
    }

    @Override
    public void doDefaultAction() throws SCException {
        // not used
    }

    @Override
    public void showHelp() {
        // intentionally left blank — no help output
    }

    @Override
    public void permissionCheck() throws SCException {
        // checked in onCommand directly
    }
}
