package com.minetexas.suffixcommands.commands;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.ChatterManager;
import com.dthielke.herochat.Herochat;
import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class ChatCommand extends CommandBase {

	@Override
	public void init() {
		command = "/chat";
		displayName = "Chat with your Badge Groups";
		
		commands.put("list", "List all badge chat channels you can access");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		init();
		
		this.args = args;
		this.sender = sender;
		
		permissionCheck();
		
		// Support toggle behavior for both /bc and /chat:
		// - /bc or /chat with no args: toggle off
		// - /bc <badge> or /chat <badge> (single arg) : enable badge-chat for that badge (unless the arg is a subcommand like 'list' or 'help')
		// - /bc <badge> <message...> or /chat <badge> <message...> : send a one-off message (handled by parse_chat)
		if (label != null && (label.equalsIgnoreCase("bc") || label.equalsIgnoreCase("chat"))) {
			if (!(sender instanceof Player)) {
				sendError(sender, "Only players can toggle badge chat.");
				return true;
			}
			Player player = (Player) sender;
			String playerUUID = player.getUniqueId().toString();
			// No args -> toggle off
			if (args.length == 0) {
				if (SCSettings.bcToggles.remove(playerUUID) != null) {
					sendMessage(sender, SCColor.Green + "Badge chat disabled.");
				} else {
					sendMessage(sender, SCColor.Yellow + "Badge chat is not enabled. Use /bc [badge] to enable.");
				}
				return true;
			}
			// Single arg -> possible toggle on (but skip if it's a subcommand like 'list' or 'help')
			if (args.length == 1) {
				String possible = args[0];
				// don't treat known subcommands as toggle
				if (possible.equalsIgnoreCase("help") || possible.equalsIgnoreCase("list") || commands.containsKey(possible.toLowerCase())) {
					// fall through to normal handling below (parse_chat will handle list/help)
				} else {
					String badgeName = possible;
					Badge badge = SCSettings.badges.get(badgeName);
					if (badge == null) {
						ConfigBadges legacyBadge = SCSettings.legacyBadges.get(badgeName);
						if (legacyBadge == null) {
							sendError(sender, "Invalid Badge Name. Use exact spelling and capitalization");
							return true;
						}
						if (!player.hasPermission(SCSettings.PERMISSION_CHAT + legacyBadge.name)) {
							sendMessage(sender, SCColor.Red + "You don't have access to the '" + legacyBadge.name
								+ "' Badge Group.");
							return true;
						}
						SCSettings.bcToggles.put(playerUUID, legacyBadge.name);
						sendMessage(sender, SCColor.Green + "Badge chat enabled for '" + legacyBadge.name
							+ "'. Use /bc or /chat with no args to turn it off.");
						return true;
					} else {
						if (!badge.canUseBadge(playerUUID)) {
							sendMessage(sender, SCColor.Red + "You don't have access to the '" + badge.getName() + "' Badge Group.");
							return true;
						}
						SCSettings.bcToggles.put(playerUUID, badge.getName());
						sendMessage(sender, SCColor.Green + "Badge chat enabled for '" + badge.getName()
							+ "'. Use /bc or /chat with no args to turn it off.");
						return true;
					}
				}
			}
		}
		
		if (args.length == 0) {
			doDefaultAction();
			return false;
		}
		
		if (args[0].equalsIgnoreCase("help")) {
			showHelp();
			return true;
		}
		
	
		try {
			parse_chat();
			return true;
		} catch (SCException e) {
			sendError(sender, e.getMessage());
			return false;
		}
	}
	
	public void parse_chat() throws SCException {		
		if (args.length < 1) {
			throw new SCException("Enter a Badge Name");
		}
		
		if (args[0].equalsIgnoreCase("list"))
		{
			list_cmd();
			return;
		}
		
		if (args.length < 2) {
			throw new SCException("You have to say something.");
		}
		Badge badge = SCSettings.badges.get(args[0]);
		if (badge == null) {
			ConfigBadges legacyBadge = SCSettings.legacyBadges.get(args[0]);
			if (legacyBadge == null) {
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}
			
			StringBuilder builder = new StringBuilder();
			args[0] = "";
			
			for(String s : args) {
				if (s.length() >= 1) {
					builder.append(" "+s);
				}
			}
			
			String message = builder.toString();
			int rc = com.minetexas.suffixcommands.util.BadgeChatRouter.routeLegacyBadgeChat((Player) sender, legacyBadge, message.substring(message.indexOf(' ')+1));
			if (rc == com.minetexas.suffixcommands.util.BadgeChatRouter.NOT_FOUND) {
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			} else if (rc == com.minetexas.suffixcommands.util.BadgeChatRouter.NO_ACCESS) {
				sendMessage(sender, SCColor.Red + "You don't have access to the '" + legacyBadge.name + "' Badge Group.");
				return;
			}
		} else {
			Player player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			
			StringBuilder builder = new StringBuilder();

			args[0] = "";

			for(String s : args) {
				if (s.length() >= 1) {
				    builder.append(" "+s);
				}
			}

			String message = builder.toString();
			int rc = com.minetexas.suffixcommands.util.BadgeChatRouter.routeBadgeChat((Player) sender, badge, message.substring(message.indexOf(' ')+1));
			if (rc == com.minetexas.suffixcommands.util.BadgeChatRouter.NO_ACCESS) {
				sendMessage(sender, SCColor.Red + "You don't have access to the '" + badge.getName() + "' Badge Group.");
				return;
			}
		}
	}
	
	public void list_cmd() throws SCException {
		Boolean hasBadges = false;
		sendHeading(sender, "List Badge Chat Channels");
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		for (Badge badge : SCSettings.badges.values()) {
			if (badge.canUseBadge(senderUUID)) {
				String status = "";
				if (badge.isLeader(senderUUID)) {
					status = "Leader";
				} else if (badge.isMember(senderUUID)) {
					status = "Member";
				} else {
					status = "Owner";
				}
				sendMessage(sender, badge.getName()+SCColor.Green+" ["+status+"]:"+ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()));
				hasBadges = true;
			}
		}
		if (!hasBadges) {
			sendMessage(sender, "You don't own any Group badges");
		}
	}

	@Override
	public void doDefaultAction() {		
		if (args.length >= 1) {
			try {
				parse_chat();
			} catch (SCException e) {
				sendError(sender, e.getCause().getMessage());
//				e.printStackTrace();
			}
		} else {
			showBasicHelp();
		}
	}

	@Override
	public void showHelp() {
		Player player;
		try {
			player = getPlayer();
		} catch (SCException e) {
			e.printStackTrace();
			return;
		}
		
		if (!player.isOp() && !player.hasPermission(SCSettings.BADGE)) {
			return;
		}
		
		showBasicHelp();

	}
	
	public void showBasicHelp() {
		sendHeading(sender, displayName);
		for (String c : commands.keySet()) {
			String info = commands.get(c);
			
			info = info.replace("[", SCColor.Yellow+"[");
			info = info.replace("]", "]"+SCColor.LightGray);
			info = info.replace("(", SCColor.Yellow+"(");
			info = info.replace(")", ")"+SCColor.LightGray);
						
			sendMessage(sender, SCColor.LightPurple+command+" "+c+SCColor.LightGray+" "+info);
		}
	}
	
	public Boolean permissionCheck(String permission) {
		Player player;
		try {
			player = getPlayer();
		} catch (SCException e) {
			e.printStackTrace();
			return false;
		}
		
		if (!player.isOp() && !player.hasPermission(permission)) {
			return false;
		}
		return true;
	}

	@Override
	public void permissionCheck() {
		// TODO Auto-generated method stub

	}

}
