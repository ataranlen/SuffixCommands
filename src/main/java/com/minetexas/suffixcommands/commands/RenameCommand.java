package com.minetexas.suffixcommands.commands;

import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.SCColor;

public class RenameCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(SCColor.Red + "You must be a player to rename a badge.");
			return true;
		}
		Player player = (Player) sender;
		if (args.length == 0) {
			sender.sendMessage(SCColor.Red + "Usage: /rename [badge] [newName] [badgeText]");
			return true;
		}
		if (args[0].equalsIgnoreCase("confirm")) {
			try {
				BadgeCommand.confirmPendingRename(player, sender);
			} catch (SCException e) {
				sender.sendMessage(SCColor.Red + e.getMessage());
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("cancel")) {
			BadgeCommand.cancelPendingRename(player, sender);
			return true;
		}
		if (args.length < 3) {
			sender.sendMessage(SCColor.Red + "Usage: /rename [badge] [newName] [badgeText]");
			return true;
		}
		String name = args[0];
		String newName = args[1];
		String badgeText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
		BadgeCommand.queueRename(player, name, newName, badgeText);
		return true;
	}
}
