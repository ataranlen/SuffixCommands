package com.minetexas.suffixcommands.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class BadgeCommand extends CommandBase {

	@Override
	public void init() {
		command = "/badge";
		displayName = "Manage Your Badges";
		
		commands.put("set", "Change your badge to one you own");
		commands.put("give", "Grant a player access to a group badge");
		commands.put("take", "Remove a player's access to a group badge");
		commands.put("share", "Grant a player access to give a group badge");
		commands.put("leave", "Leave a group badge");
		commands.put("remove", "Remove your current badge");
		commands.put("owned", "List all your owned badges");
		commands.put("group", "List all your group badges");
		commands.put("list", "List all possible badges");
		commands.put("members", "List all members of the named badge group");
		commands.put("create", "Create a new badge group");
		commands.put("rename", "Rename a badge group");
		
		commands.put("reload", "Reload Badges from the Config [Admin Only]");
	}
	
	public void set_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid Badge Name. Use /badge set [name]");
		}
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			ConfigBadges legacyBadge = SCSettings.legacyBadges.get(args[1]);
			if (legacyBadge == null)
			{
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}
			
			if (permissionCheck(SCSettings.PERMISSION_BASE+legacyBadge.name)
					|| permissionCheck(SCSettings.GROUPSHARE_BASE+legacyBadge.name)
					|| permissionCheck(SCSettings.GROUP_BASE+legacyBadge.name)) {
				Player player;
				try {
					player = getPlayer();
					sendMessage(sender, SCColor.Green+"Badge Set to:"+ChatColor.translateAlternateColorCodes('&', legacyBadge.badgeText));
					String command = "pex user "+player.getName()+" suffix \""+legacyBadge.badgeText+"\"";
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
				} catch (SCException e) {
					e.printStackTrace();
				}
				
			} else {
				throw new SCException("You don't own the "+legacyBadge.name+" Badge.");
			}
		} else {
			Player player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			if (badge.canUseBadge(playerUUID)) {
				sendMessage(sender, SCColor.Green+"Badge Set to:"+ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()));
				String command = "pex user "+player.getName()+" suffix \""+badge.getBadgeText()+"\"";
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			}else {
				sendMessage(sender, SCColor.Red+"You don't have 'use' access to the "+badge.getName()+" Badge.");
			}
		}
	}
	
	public void give_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		}
		if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		}
		if (args.length > 3) {
			throw new SCException("Invalid Badge Name. Use /badge give [name] [player]");
		}
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		String playerName = args[2];
		
		@SuppressWarnings("deprecation")
		OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();
		
		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canUseBadge(playerUUID)) {
				throw new SCException(playerName+" already has 'use' access to the '"+badge.getName()+"' Badge Group.");
			}

			badge.addMemberUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen+playerName+" was given 'use' access to the '"+badge.getName()+"' Badge Group.");
			return;
		}
		
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '"+badge.getName()+"' Badge Group.");
		}

		if (badge.canGiveBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canUseBadge(playerUUID)) {
				throw new SCException(playerName+" already has 'use' access to the '"+badge.getName()+"' Badge Group.");
			}

			badge.addMemberUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen+playerName+" was given 'use' access to the '"+badge.getName()+"' Badge Group.");
		} else {
			sendMessage(sender, SCColor.Red+"You don't have 'give' access to the '"+badge.getName()+"' Badge Group.");
		}
	}
	
	public void share_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		}
		if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		}
		if (args.length > 3) {
			throw new SCException("Invalid Badge Name. Use /badge share [name] [player]");
		}
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		String playerName = args[2];
		
		@SuppressWarnings("deprecation")
		OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();
		
		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canGiveBadge(playerUUID)) {
				throw new SCException(playerName+" already has 'give' access to the '"+badge.getName()+"' Badge Group.");
			}

			badge.addLeaderUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen+playerName+" was given 'give' access to the '"+badge.getName()+"' Badge Group.");
			return;
		}
		
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '"+badge.getName()+"' Badge Group.");
		}

		if (badge.canGiveBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canGiveBadge(playerUUID)) {
				throw new SCException(playerName+" already has 'give' access to the '"+badge.getName()+"' Badge Group.");
			}

			badge.addLeaderUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen+playerName+" was given 'give' access to the '"+badge.getName()+"' Badge Group.");
		} else {
			sendMessage(sender, SCColor.Red+"You don't have 'share' access to the '"+badge.getName()+"' Badge Group.");
		}
		
	}
	
	public void take_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		}
		if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		}
		if (args.length > 3) {
			throw new SCException("Invalid Badge Name. Use /badge take [name] [player]");
		}
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		String playerName = args[2];
		
		@SuppressWarnings("deprecation")
		OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();
		
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot remove yourself from the '"+badge.getName()+"' Badge Group.");
		}
		
		if (badge.canShareBadge(senderUUID)) {
			sendMessage(sender, SCColor.Green+"Removed "+player+"'s 'give' access to the '"+badge.getName()+"' Badge Group");

			badge.removeLeaderUUID(playerUUID);
		    PermissionUser user = PermissionsEx.getUser(playerName);

			if (user != null) {
				String suffix = user.getSuffix();
				if (suffix.equals(" "+badge.getBadgeText())) {
					String clearSuffix = "pex user "+player+" suffix \"\"";
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), clearSuffix);
				}
			}
		
		} else if (badge.canGiveBadge(senderUUID)) {
			if (badge.canShareBadge(playerUUID)) {
				throw new SCException("You cannot remove the owner from the '"+badge.getName()+"' Badge Group.");
			} else if (badge.canGiveBadge(playerUUID)) {
				throw new SCException("You cannot remove another Leader from the '"+badge.getName()+"' Badge Group.");
			}
			sendMessage(sender, SCColor.Green+"Removed "+player+"'s 'use' access to the '"+badge.getName()+"' Badge Group");

			badge.removeMemberUUID(playerUUID);
		} else {
			sendMessage(sender, SCColor.Red+"You don't have 'share' access to the '"+badge.getName()+"' Badge Group.");
		}
	}
	
	public void leave_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid Badge Name. Use /badge leave [name]");
		}
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		
		if (badge.canShareBadge(senderUUID)) {
			throw new SCException("You are the leader of the '"+badge.getName()+"' Badge Group. You cannot leave.");
		} else if (badge.canUseBadge(senderUUID)) {
			if (badge.canGiveBadge(senderUUID)) {
				badge.removeLeaderUUID(senderUUID);
			}
			badge.removeMemberUUID(senderUUID);
			
			PermissionUser user = PermissionsEx.getUser(sender);

			if (user != null) {
				String suffix = user.getSuffix();
				if (suffix.equals(" "+badge.getBadgeText())) {
					String clearSuffix = "pex user "+sender.getName()+" suffix \"\"";
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), clearSuffix);
				}
			}

			sendMessage(sender, SCColor.Red+"You have given up access to the '"+badge.getName()+"' Badge Group.");
		} else {
			sendMessage(sender, SCColor.Red+"You already don't have access to the '"+badge.getName()+"' Badge Group.");
		}
	}
	
	public void list_cmd() throws SCException {
		sendHeading(sender, "List of all Legacy Badges");
		sendMessage(sender, SCColor.Yellow+"Buy Badges: "+SCColor.Green+"http://buy.minetexas.com/category/572289");
		for (ConfigBadges badge : SCSettings.legacyBadges.values())
		{
			if (permissionCheck(SCSettings.PERMISSION_BASE+badge.name) || permissionCheck(SCSettings.GROUP_BASE+badge.name))
			{
				sendMessage(sender, badge.name+SCColor.Green+" [Owned]:"+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			} else {
				sendMessage(sender, badge.name+SCColor.LightGray+" [Unowned]:"+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			}
		}
	}
	
	public void group_cmd() throws SCException {
		Boolean hasBadges = false;
		sendHeading(sender, "List of owned Group Badges");
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
	
	public void owned_cmd() throws SCException {
		Boolean hasBadges = false;
		sendHeading(sender, "List of owned Badges");
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
			} 
		}
		for (ConfigBadges badge : SCSettings.legacyBadges.values())
		{
			if (permissionCheck(SCSettings.PERMISSION_BASE+badge.name) || permissionCheck(SCSettings.GROUP_BASE+badge.name))
			{
				hasBadges = true;
				sendMessage(sender, badge.name+":"+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			}
		}
		
		if (!hasBadges) {
			sendMessage(sender, "You don't own any badges");
		}
	}
	
	public void remove_cmd() throws SCException {		

		Player player;
		try {
			player = getPlayer();
			sendMessage(sender, SCColor.LightGreen+"Badge removed");
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "pex user "+player.getName()+" suffix \"\"");
		} catch (SCException e) {
			e.printStackTrace();
		}
	}
	
	public void members_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a group Badge Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid group Badge Name. Use /badge members [name]");
		}
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization");
		}

		Player player = getPlayer();
		String playerUUID = player.getUniqueId().toString();
		if (badge.canUseBadge(playerUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			//List all players who have access to the badge group.
			sendMessage(sender, SCColor.Green+"[Owner]: "+SCColor.ITALIC+Bukkit.getServer().getOfflinePlayer(UUID.fromString(badge.getOwnerUUID())).getName());
			
			ArrayList<String> leaders = badge.getLeaderUUIDs();
			if (!leaders.isEmpty()) {
				String leaderString = SCColor.LightGreen+"[Leaders]: "+SCColor.ITALIC;
				for (String uuidString : leaders) {
					leaderString += Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName()+", ";  
				}

				sendMessage(sender, leaderString);
			}
			ArrayList<String> members = badge.getLeaderUUIDs();
			if (!members.isEmpty()) {
				String memberString = SCColor.White+"[Members]: "+SCColor.ITALIC;

				for (String uuidString : badge.getMemberUUIDs()) {
					memberString += Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName()+", ";  
					
				}
				sendMessage(sender, memberString);
			}
		}else {
			sendMessage(sender, SCColor.Red+"You don't have 'use' access to the "+badge.getName()+" Badge.");
		}
		
	}
	
	public void create_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		} else if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		} else if (args.length < 4) {
			throw new SCException("Enter badge display text");
		} else if (args.length < 5) {
			throw new SCException("Enter badge chat color code [&2]");
		} else if (args.length > 5) {
			throw new SCException("Invalid Badge Name. Use /badge create [name] [player] [displayText] [colorCode]");
		}
		String name = args[1];
		String playerName = args[2];
		String badgeText = args[3];
		String colorCode = args[4];
		
		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			Badge badge = SCSettings.badges.get(args[1]);
			if (badge == null) {

				@SuppressWarnings("deprecation")
				OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
				String playerUUID = player.getUniqueId().toString();
				
				
				try {
					Badge newBadge = new Badge(name, " "+badgeText, colorCode, playerUUID);
					SCLog.debug("PlayerName:"+ playerName +"; UUID: "+playerUUID);
					try {
						newBadge.saveNow();
						SCSettings.badges.put(name, newBadge);
					} catch (SQLException e) {
						throw new SCException("Badge save failed");
					}

					sendMessage(sender, SCColor.Red+"Badge Created");
				} catch (InvalidNameException e) {
					e.printStackTrace();

					throw new SCException("Badge create failed");
				}

				
			} else {

				throw new SCException("Badge already exists");
			}
		} else {

			sendMessage(sender, SCColor.Red+"You don't have 'create' access for Badge Groups.");
		}
	}
	
	public void rename_cmd() throws SCException {
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		} else if (args.length < 3) {
			throw new SCException("Enter the new Badge Name");
		} else if (args.length < 4) {
			throw new SCException("Enter badge display text including color codes");
		} else if (args.length > 4) {
			throw new SCException("Invalid Badge Name. Use /badge rename [name] [newName] [displayText]");
		}
		String name = args[1];
		String newName = args[2];
		String badgeText = args[3];

		newName = newName.replaceAll("§", "");
		newName = newName.replaceAll("&", "");
		badgeText = badgeText.replaceAll("§", "&");
		badgeText = badgeText.replaceAll("&k", "");
		if (newName.length() >= 10) {

			throw new SCException("Please limit your New name to 10 characters.");
		}
		if (badgeText.length() >= 12) {

			throw new SCException("Please limit your displayText to 12 characters.");
		}
		
		Player player = getPlayer();
		String playerUUID = player.getUniqueId().toString();
		
		Badge badge = SCSettings.badges.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization");
		}

		if (badge.canShareBadge(playerUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			
			Badge newBadge = SCSettings.badges.get(newName);
			if (newBadge == null || name.equals(newName)) {
				try {
					badge.rename(newName, " "+badgeText);
					sendMessage(sender, SCColor.Green+"'"+name+"' has been renamed to '"+newName+"' with the badgeText of '"+ChatColor.translateAlternateColorCodes('&', badgeText)+SCColor.Green+"'");
				} catch (InvalidNameException e) {
					SCLog.exception("Badge Save failed", e);
					throw new SCException("Badge save failed, Contact an admin.");
				}
			} else {
				throw new SCException("Badge already exists");
			}
		}
	}
	
	public void reload_cmd() throws SCException {
		try {
			SCSettings.reloadBadgeConfigFile();
			sendMessage(sender, "Badges reloaded");
		} catch (IOException | InvalidConfigurationException
				| InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}

	@Override
	public void doDefaultAction() {
		// TODO Auto-generated method stub
		showBasicHelp();
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
		if (sender instanceof Player) {
			Player player;
			try {
				player = getPlayer();
				if (player.isOp() || player.hasPermission(permission)) {
					return true;
				}
			} catch (SCException e) {
				e.printStackTrace();
				
			}
			
		}
		
		return sender.isOp();
	}

	@Override
	public void permissionCheck() {
		// TODO Auto-generated method stub

	}

}
