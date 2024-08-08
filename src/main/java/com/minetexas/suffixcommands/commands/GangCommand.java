package com.minetexas.suffixcommands.commands;

import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.Gang;
import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class GangCommand extends CommandBase {

	@Override
	public void init() {
		command = "/gang";
		displayName = "Manage Your Gangs. Gang names are case sensitive.";
		
		commands.put("set", "Change your gang to one you can use. Usage: /gang set [gang]");
		commands.put("give", "Grant a player access to a gang. Usage: /gang give [gang] [player]");
		commands.put("take", "Remove a player's access to a gang. Usage: /gang take [gang] [player]");
		commands.put("share", "Grant a player access to give a gang. Usage: /gang share [gang] [player]");
		commands.put("leave", "Leave a gang");
		commands.put("remove", "Remove your current gang");
		commands.put("owned", "List all your owned gangs");
		commands.put("group", "List all your gangs");
		commands.put("members", "List all members of the named gang");
		commands.put("setowner", "Transfer Ownership of the gang. Usage: /gang setowner [gang] [newOwnerName]");
		commands.put("create", "Create a new gang. [Admin Only] Usage: /gang create [name] [owner] [gangText] [Cgang Color Code]");
		commands.put("rename", "Rename a gang. [Admin Only] Usage: /gang rename [name] [newname] [gangText]");
		commands.put("delete", "Deletes a gang [Admin Only]");
	}
	
	public void set_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid Gang Name. Use /gang set [name]");
		}
		
		Gang gang = SCSettings.gangs.get(args[1]);
		if (gang == null) {
			throw new SCException("Invalid Gang Name. Use exact spelling and capitalization");
		} else {
			Player player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			if (gang.canUseGang(playerUUID)) {
				sendMessage(sender, SCColor.Green+"Gang Set to: "+ChatColor.translateAlternateColorCodes('&', gang.getGangText()));

				User user = SCSettings.luckPermsAPI.getUserManager().getUser(player.getUniqueId());
				SCSettings.setSuffix(user, gang.getGangText());

			} else {
				sendMessage(sender, SCColor.Red+"You don't have 'use' access to the "+gang.getName()+" Gang.");
			}
		}
	}
	
	public void give_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		}
		if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		}
		if (args.length > 3) {
			throw new SCException("Invalid Gang Name. Use /gang give [name] [player]");
		}
		
		Gang gang = SCSettings.gangs.get(args[1]);
		if (gang == null) {
			throw new SCException("Invalid Gang Name. Use exact spelling and capitalization");
		}
		String playerName = args[2];
		
		@SuppressWarnings("deprecation")
		OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();
		if (existingGangMember(playerUUID)) {
			throw new SCException(playerName+" is already a member of a Gang.");
		}
		
		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (gang.canUseGang(playerUUID)) {
				throw new SCException(playerName+" already has 'use' access to the '"+gang.getName()+"' Gang.");
			}

			gang.addMemberUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen+playerName+" was given 'use' access to the '"+gang.getName()+"' Gang.");
			return;
		}
		
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '"+gang.getName()+"' Gang.");
		}

		if (gang.canGiveGang(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (gang.canUseGang(playerUUID)) {
				throw new SCException(playerName+" already has 'use' access to the '"+gang.getName()+"' Gang.");
			}

			gang.addMemberUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen+playerName+" was given 'use' access to the '"+gang.getName()+"' Gang.");
		} else {
			sendMessage(sender, SCColor.Red+"You don't have 'give' access to the '"+gang.getName()+"' Gang.");
		}
	}
	
	public void share_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		}
		if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		}
		if (args.length > 3) {
			throw new SCException("Invalid Gang Name. Use /gang share [name] [player]");
		}
		
		Gang gang = SCSettings.gangs.get(args[1]);
		if (gang == null) {
			throw new SCException("Invalid Gang Name. Use exact spelling and capitalization");
		}
		String playerName = args[2];
		
		@SuppressWarnings("deprecation")
		OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();
		if (existingGangMember(playerUUID)) {
			throw new SCException(playerName + " is already a member of a Gang.");
		}
		
		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (gang.canGiveGang(playerUUID)) {
				throw new SCException(playerName + " already has 'give' access to the '"+gang.getName()+"' Gang.");
			}

			gang.addLeaderUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen + playerName + " was given 'give' access to the '" + gang.getName() + "' Gang.");
			return;
		}
		
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '" + gang.getName() + "' Gang.");
		}

		if (gang.canGiveGang(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (gang.canGiveGang(playerUUID)) {
				throw new SCException(playerName+" already has 'give' access to the '" + gang.getName() + "' Gang.");
			}

			gang.addLeaderUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen + playerName + " was given 'give' access to the '" + gang.getName() + "' Gang.");
		} else {
			sendMessage(sender, SCColor.Red + "You don't have 'share' access to the '" + gang.getName() + "' Gang.");
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
			throw new SCException("Invalid Badge Name. Use /gang take [name] [player]");
		}

		Gang gang = SCSettings.gangs.get(args[1]);
		if (gang == null) {
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		String playerName = args[2];

		try {

			@SuppressWarnings("deprecation")
			OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
			Player sender = getPlayer();

			String senderUUID = sender.getUniqueId().toString();
			String playerUUID = player.getUniqueId().toString();

			if (senderUUID.equals(playerUUID)) {
				throw new SCException("You cannot remove yourself from the '" + gang.getName() + "' Badge Group.");
			}
			if (gang.canShareGang(senderUUID) && gang.canShareGang(playerUUID)) {
				sendMessage(sender, SCColor.Green+"Removed "+player+"'s 'give' access to the '" + gang.getName() + "' Badge Group");

				gang.removeLeaderUUID(playerUUID);
				User user = SCSettings.luckPermsAPI.getUserManager().getUser(player.getUniqueId());
				String prefix = SCSettings.getPrefix(user);
				if (prefix.equals(gang.getGangText())) {
					CommandSender server = Bukkit.getConsoleSender();
					Bukkit.getServer().dispatchCommand(server, "lp user " + user.getUsername() + " meta clear prefixes");
				}

			} else if (gang.canGiveGang(senderUUID)) {
				if (gang.canShareGang(playerUUID)) {
					throw new SCException("You cannot remove the owner from the '" + gang.getName() + "' Gang.");
				} else if (gang.canGiveGang(playerUUID)) {
					throw new SCException("You cannot remove another Leader from the '" + gang.getName() + "' Gang.");
				}
				sendMessage(sender, SCColor.Green+"Removed " + player + "'s 'use' access to the '" + gang.getName() + "' Gang.");

				gang.removeMemberUUID(playerUUID);
			} else {
				sendMessage(sender, SCColor.Red+"You don't have 'share' access to the '" + gang.getName() + "' Gang.");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void leave_cmd() throws SCException {
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid Gang Name. Use /gang leave [name]");
		}

		Gang badge = SCSettings.gangs.get(args[1]);
		if (badge == null) {
			throw new SCException("Invalid Gang Name. Use exact spelling and capitalization");
		}

		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();

		if (badge.canShareGang(senderUUID)) {
			throw new SCException("You are the leader of the '" + badge.getName() + "' Gang. You cannot leave.");
		} else if (badge.canUseGang(senderUUID)) {
			if (badge.canGiveGang(senderUUID)) {
				badge.removeLeaderUUID(senderUUID);
			}
			badge.removeMemberUUID(senderUUID);

			User user = SCSettings.luckPermsAPI.getUserManager().getUser(senderUUID);
			String suffix = SCSettings.getSuffix(user);

			if (suffix.equals(badge.getGangText())) {
				CommandSender server = Bukkit.getServer().getConsoleSender();
				Bukkit.dispatchCommand(server, "lp user " + user.getUsername() + " meta clear prefixes");
			}

			sendMessage(sender, SCColor.Red + "You have given up access to the '" + badge.getName() + "' Gang Group.");
		} else {
			sendMessage(sender, SCColor.Red + "You already don't have access to the '" + badge.getName() + "' Gang Group.");
		}
	}
	
	public void group_cmd() throws SCException {
		Boolean hasGangs = false;
		sendHeading(sender, "List of owned Gangs");
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		for (Gang gang : SCSettings.gangs.values()) {
			if (gang.canUseGang(senderUUID)) {
				String status = "";
				if (gang.isLeader(senderUUID)) {
					status = "Leader";
				} else if (gang.isMember(senderUUID)) {
					status = "Member";
				} else {
					status = "Owner";
				}
				sendMessage(sender, gang.getName()+SCColor.Green+" ["+status+"]:"+ChatColor.translateAlternateColorCodes('&', gang.getGangText()));
				hasGangs = true;
			}
		}
		if (!hasGangs) {
			sendMessage(sender, "You don't own any Group gangs");
		}
	}
	
	public void owned_cmd() throws SCException {
		Boolean hasGangs = false;
		sendHeading(sender, "List of owned Gangs");
		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		
		for (Gang gang : SCSettings.gangs.values()) {
			if (gang.canUseGang(senderUUID)) {
				hasGangs = true;
				String status = "";
				if (gang.isLeader(senderUUID)) {
					status = "Leader";
				} else if (gang.isMember(senderUUID)) {
					status = "Member";
				} else {
					status = "Owner";
				}
				sendMessage(sender, gang.getName()+SCColor.Green+" ["+status+"]:"+ChatColor.translateAlternateColorCodes('&', gang.getGangText()));
			} 
		}
		
		if (!hasGangs) {
			sendMessage(sender, "You don't own any gangs");
		}
	}
	
	public void remove_cmd() throws SCException {		
		if (args.length == 2) {
			if (!permissionCheck(SCSettings.PERMISSION_CREATE)) {
				throw new SCException("Invalid Gang Name");
			}
			String playerName = args[1];
			@SuppressWarnings("deprecation")
			OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		    User user = SCSettings.luckPermsAPI.getUserManager().getUser(playerName);
			sendMessage(sender, SCColor.LightGreen+playerName+"'s Gang removed");
			CommandSender server = Bukkit.getServer().getConsoleSender();
			Bukkit.getServer().dispatchCommand(server, "lp user " + player.getName() + " meta setprefix 100 \"" + existingPlayerPrefix(user) + "\"");
			return;
		}
		Player player;
		try {
			player = getPlayer();
			sendMessage(sender, SCColor.LightGreen+"Gang removed");

			CommandSender server = Bukkit.getServer().getConsoleSender();
			Bukkit.getServer().dispatchCommand(server, "lp user "+ player.getName() +" meta clear prefixes");

		} catch (SCException e) {
			e.printStackTrace();
		}
	}
	
	public void members_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid Gang Name. Use /gang members [name]");
		}
		
		Gang gang = SCSettings.gangs.get(args[1]);
		if (gang == null) {
			throw new SCException("Invalid Gang Name. Use exact spelling and capitalization");
		}

		Player player = getPlayer();
		String playerUUID = player.getUniqueId().toString();
		if (gang.canUseGang(playerUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			//List all players who have access to the gang group.
			sendMessage(sender, SCColor.Green+"[Owner]: "+SCColor.ITALIC+Bukkit.getServer().getOfflinePlayer(UUID.fromString(gang.getOwnerUUID())).getName());
			
			ArrayList<String> leaders = gang.getLeaderUUIDs();
			if (!leaders.isEmpty()) {
				String leaderString = SCColor.LightGreen+"[Leaders]: "+SCColor.ITALIC;
				for (String uuidString : leaders) {
					leaderString += Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName()+", ";  
				}

				sendMessage(sender, leaderString);
			}
			ArrayList<String> members = gang.getMemberUUIDs();
			if (!members.isEmpty()) {
				String memberString = SCColor.White+"[Members]: "+SCColor.ITALIC;

				for (String uuidString : gang.getMemberUUIDs()) {
					memberString += Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName()+", ";  
					
				}
				sendMessage(sender, memberString);
			}
		}else {
			sendMessage(sender, SCColor.Red+"You don't have 'use' access to the "+gang.getName()+" Gang.");
		}
		
	}
	
	public void create_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		} else if (args.length < 3) {
			throw new SCException("Enter a Player Name");
		} else if (args.length < 4) {
			throw new SCException("Enter gang display text");
		} else if (args.length > 4) {
			throw new SCException("Invalid Gang Name. Use /gang create [name] [player] [displayText]");
		}
		String name = args[1];
		String playerName = args[2];
		String gangText = args[3];

		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			Gang gang = SCSettings.gangs.get(args[1]);
			if (gang == null) {

				@SuppressWarnings("deprecation")
				OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
				String playerUUID = player.getUniqueId().toString();
				
				
				try {
					Gang newGang = new Gang(name, gangText, playerUUID);
					SCLog.debug("PlayerName:"+ playerName +"; UUID: "+playerUUID);
					try {
						newGang.saveNow();
						SCSettings.gangs.put(name, newGang);
					} catch (SQLException e) {
						throw new SCException("Gang save failed");
					}
					sendMessage(sender, SCColor.Red+"Gang Created");
				} catch (InvalidNameException e) {
					e.printStackTrace();
					throw new SCException("Gang create failed");
				}
			} else {
				throw new SCException("Gang already exists");
			}
		} else {

			sendMessage(sender, SCColor.Red+"You don't have 'create' access for Gang Groups.");
		}
	}
	
	public void rename_cmd() throws SCException {
		if (args.length < 2) {
			throw new SCException("Enter a Gang Name");
		} else if (args.length < 3) {
			throw new SCException("Enter the new Gang Name");
		} else if (args.length < 4) {
			throw new SCException("Enter gang display text including color codes");
		} else if (args.length > 4) {
			throw new SCException("Invalid Gang Name. Use /gang rename [name] [newName] [displayText]");
		}
		String name = args[1];
		String newName = args[2];
		String gangText = args[3];

		newName = newName.replaceAll("�", "");
		newName = newName.replaceAll("&", "");
		gangText = gangText.replaceAll("�", "&");
		gangText = gangText.replaceAll("&k", "");
		if (newName.length() >= 16 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

			throw new SCException("Please limit your New name to 10 characters.");
		}
		if (gangText.length() >= 20 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

			throw new SCException("Please limit your displayText to 12 characters.");
		}
		
		Gang gang = SCSettings.gangs.get(args[1]);
		if (gang == null) {
			throw new SCException("Invalid Group Gang Name. Use exact spelling and capitalization");
		}

		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			Gang newGang = SCSettings.gangs.get(newName);
			if (newGang == null || name.equals(newName)) {
				try {
					gang.rename(newName, gangText);
					sendMessage(sender, SCColor.Green+"'"+name+"' has been renamed to '"+newName+"' with the gangText of '"+ ChatColor.translateAlternateColorCodes('&', gangText)+SCColor.Green+"'");
				} catch (InvalidNameException e) {
					SCLog.exception("Gang Save failed", e);
					throw new SCException("Gang save failed, Contact an admin.");
				}
			} else {
				throw new SCException("Gang already exists");
			}
		}
	}
	
	public void setowner_cmd() throws SCException {
		if (this.args.length < 2) {
			throw new SCException("Enter a Gang Name. /gang setowner [gang] [player]");
		}
		if (this.args.length < 3) {
			throw new SCException("Enter the new Owner for the gang. Use /gang setowner [gang] [player]");
		}
		String playerName = this.args[2];
		Player player = getPlayer();
		String playerUUID = player.getUniqueId().toString();

		@SuppressWarnings("deprecation")
		OfflinePlayer newPlayer = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String newplayerUUID = newPlayer.getUniqueId().toString();
		Gang badge = (Gang)SCSettings.gangs.get(this.args[1]);
		if (badge == null)
			throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
		if (badge.canShareGang(playerUUID).booleanValue() || permissionCheck("suffixcommands.createbadges").booleanValue())
		  try {
		    badge.changeHands(newplayerUUID);
			sendMessage(sender, SCColor.Green+"Changed owner of '"+badge.getName()+"' Gang to '" + newPlayer.getName());
		  } catch (SQLException e) {
		    e.printStackTrace();
		  }  
	}
	
	public void delete_cmd() throws SCException {
		try {
			if (!permissionCheck(SCSettings.PERMISSION_CREATE))
				throw new SCException("You must be an Admin to do this"); 
			if (this.args.length != 2)
				throw new SCException("Enter a group Badge Name"); 
			Gang badge = (Gang)SCSettings.gangs.get(this.args[1]);
			if (badge == null)
				throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
	        	badge.delete();
				sendMessage(sender, SCColor.Red+"Deleted '"+badge.getName()+"' Gang.");

    	} catch (SQLException e) {
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
		if (!permissionCheck(SCSettings.HAT)) {
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

	public String existingPlayerPrefix(User player) {
		String prefix = "";
		CachedPermissionData userPerms = player.getCachedData().getPermissionData();
		if (userPerms.checkPermission("permission.group.owner").asBoolean()) {
			SCLog.info("Admin, no prefix");
			return "&4";
		} else if (userPerms.checkPermission("permission.group.plusgold").asBoolean()) {
			prefix += "&6&l+";
		} else if (userPerms.checkPermission("permission.group.plus").asBoolean()) {
			prefix += "&3+";
		}

		if (userPerms.checkPermission("prefix.color.&1").asBoolean()) {
			prefix += "&1";
		} else if (userPerms.checkPermission("prefix.color.&2").asBoolean()) {
			prefix += "&2";
		} else if (userPerms.checkPermission("prefix.color.&3").asBoolean()) {
			prefix += "&3";
		} else if (userPerms.checkPermission("prefix.color.&4").asBoolean()) {
			prefix += "&4";
		} else if (userPerms.checkPermission("prefix.color.&5").asBoolean()) {
			prefix += "&5";
		} else if (userPerms.checkPermission("prefix.color.&6").asBoolean()) {
			prefix += "&6";
		} else if (userPerms.checkPermission("prefix.color.&7").asBoolean()) {
			prefix += "&7";
		} else if (userPerms.checkPermission("prefix.color.&8").asBoolean()) {
			prefix += "&8";
		} else if (userPerms.checkPermission("prefix.color.&9").asBoolean()) {
			prefix += "&9";
		} else if (userPerms.checkPermission("prefix.color.&a").asBoolean()) {
			prefix += "&a";
		} else if (userPerms.checkPermission("prefix.color.&b").asBoolean()) {
			prefix += "&b";
		} else if (userPerms.checkPermission("prefix.color.&c").asBoolean()) {
			prefix += "&c";
		} else if (userPerms.checkPermission("prefix.color.&d").asBoolean()) {
			prefix += "&d";
		} else if (userPerms.checkPermission("prefix.color.&e").asBoolean()) {
			prefix += "&e";
		} else {
			prefix += "&f";
		}

		if (userPerms.checkPermission("prefix.style.&l").asBoolean()) {
			prefix += "&l";
		}
		if (userPerms.checkPermission("prefix.style.&o").asBoolean()) {
			prefix += "&o";
		}
		if (userPerms.checkPermission("prefix.style.&m").asBoolean()) {
			prefix += "&m";
		}
		if (userPerms.checkPermission("prefix.style.&n").asBoolean()) {
			prefix += "&n";
		}

		return prefix;
	}

	public String existingPlayerPrefix(Player player) {
		String prefix = "";
		if (player.hasPermission("permission.group.owner")) {
			SCLog.info("Admin, no prefix");
			return "&4";
		} else if (player.hasPermission("permission.group.plusgold")) {
			prefix += "&6&l+";
		} else if (player.hasPermission("permission.group.plus")) {
			prefix += "&3+";
		}

		if (player.hasPermission("prefix.color.&1")) {
			prefix += "&1";
		} else if (player.hasPermission("prefix.color.&2")) {
			prefix += "&2";
		} else if (player.hasPermission("prefix.color.&3")) {
			prefix += "&3";
		} else if (player.hasPermission("prefix.color.&4")) {
			prefix += "&4";
		} else if (player.hasPermission("prefix.color.&5")) {
			prefix += "&5";
		} else if (player.hasPermission("prefix.color.&6")) {
			prefix += "&6";
		} else if (player.hasPermission("prefix.color.&7")) {
			prefix += "&7";
		} else if (player.hasPermission("prefix.color.&8")) {
			prefix += "&8";
		} else if (player.hasPermission("prefix.color.&9")) {
			prefix += "&9";
		} else if (player.hasPermission("prefix.color.&a")) {
			prefix += "&a";
		} else if (player.hasPermission("prefix.color.&b")) {
			prefix += "&b";
		} else if (player.hasPermission("prefix.color.&c")) {
			prefix += "&c";
		} else if (player.hasPermission("prefix.color.&d")) {
			prefix += "&d";
		} else if (player.hasPermission("prefix.color.&e")) {
			prefix += "&e";
		} else {
			prefix += "&f";
		}

		if (player.hasPermission("prefix.style.&l")) {
			prefix += "&l";
		}
		if (player.hasPermission("prefix.style.&o")) {
			prefix += "&o";
		}
		if (player.hasPermission("prefix.style.&m")) {
			prefix += "&m";
		}
		if (player.hasPermission("prefix.style.&n")) {
			prefix += "&n";
		}

		return prefix;
	}

	public Boolean existingGangMember(String targetUUID) {
		for (Gang gang : SCSettings.gangs.values()) {
			if (gang.canUseGang(targetUUID)) {
				return true;
			}
		}
		return false;
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
			return false;
		}
		return true;
	}

	@Override
	public void permissionCheck() {
		// TODO Auto-generated method stub

	}

}
