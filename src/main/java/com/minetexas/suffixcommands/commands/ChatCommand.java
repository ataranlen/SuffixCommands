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
			if (legacyBadge == null)
			{
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}
			
			if (permissionCheck(SCSettings.PERMISSION_CHAT+legacyBadge.name)) {			
				StringBuilder builder = new StringBuilder();
				String mainColor = ChatColor.translateAlternateColorCodes('&', legacyBadge.chatColor);
				builder.append(mainColor+"["+sender.getName()+ChatColor.translateAlternateColorCodes('&', legacyBadge.badgeText)+mainColor+"]"+mainColor);
				
				args[0] = "";
				
				for(String s : args) {
					if (s.length() >= 1) {
						builder.append(" "+s);
					}
				}
				
				String message = builder.toString();
				SCLog.info(message);

				Collection <? extends Player> players = Bukkit.getOnlinePlayers();

				Boolean useHerochat = false;
				if (SCSettings.hasHerochat == true) {
					Herochat hc = Herochat.getPlugin();
					useHerochat = hc.isEnabled();
				}
				for(Player p : players) {
					if (useHerochat) {
						ChatterManager cm = Herochat.getChatterManager();
						Player player = (Player) sender;
						Chatter chatter = cm.getChatter(player);
						Chatter chattee = cm.getChatter(p);
						if (chattee.isIgnoring(chatter)) {
							continue;
						}
					}
					if (p.hasPermission(SCSettings.PERMISSION_CHAT+legacyBadge.name)) {
						p.sendMessage(message);	
					}
				}
				
				
				
				
			} else {
				sendMessage(sender, SCColor.Red+"You don't have access to the '"+legacyBadge.name+"' Badge Group.");
			}
		} else {
			Player player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			if (badge.canUseBadge(playerUUID)) {			
				StringBuilder builder = new StringBuilder();
				String mainColor = ChatColor.translateAlternateColorCodes('&', badge.getChatColor());
				builder.append(mainColor+"["+sender.getName()+ChatColor.translateAlternateColorCodes('&',  badge.getBadgeText())+mainColor+"]"+mainColor);
				
				args[0] = "";
				
				for(String s : args) {
					if (s.length() >= 1) {
						builder.append(" "+s);
					}
				}
				
				String message = builder.toString();
				SCLog.info(message);

				Collection <? extends Player> players = Bukkit.getOnlinePlayers();

				Boolean useHerochat = false;
				if (SCSettings.hasHerochat == true) {
					Herochat hc = Herochat.getPlugin();
					useHerochat = hc.isEnabled();
				}
				for(Player p : players) {
					if (useHerochat) {
						ChatterManager cm = Herochat.getChatterManager();
						Chatter chatter = cm.getChatter(player);
						Chatter chattee = cm.getChatter(p);
						if (chattee.isIgnoring(chatter)) {
							continue;
						}
					}
					if (badge.canUseBadge(p.getUniqueId().toString())) {
						p.sendMessage(message);	
					}
				}
				
				
				
				
			} else {
				sendMessage(sender, SCColor.Red+"You don't have access to the '"+badge.getName()+"' Badge Group.");
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
