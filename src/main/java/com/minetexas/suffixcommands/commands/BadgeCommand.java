package com.minetexas.suffixcommands.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import com.minetexas.suffixcommands.SuffixCommands;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class BadgeCommand extends CommandBase {

	@Override
	public void init() {
		command = "/badge";
		displayName = "Manage Your Badges. Badge names are case sensitive.";

		commands.put("set", "Change your badge to one you own. Usage: /badge set [badge]");
		commands.put("give", "Grant a player access to a group badge. Usage: /badge give [badge] [player]");
		commands.put("take", "Remove a player's access to a group badge. Usage: /badge take [badge] [player]");
		commands.put("share", "Grant a player access to give a group badge. Usage: /badge share [badge] [player]");
		commands.put("leave", "Leave a group badge");
		commands.put("remove", "Remove your current badge");
		commands.put("owned", "List all your owned badges");
		commands.put("group", "List all your group badges");
		commands.put("list", "List all possible badges");
		commands.put("members", "List all members of the named badge group");
		commands.put("setowner", "Transfer Ownership of the badge. Usage: /badge setowner [badge] [newOwnerName]");
		commands.put("create", "Create a new badge group. [Admin Only] Usage: /badge create [badge] [owner] [badgeText] [Chat Color Code]");
		commands.put("rename", "Rename a badge group. Usage: /badge rename [badge] [newname] [badgeText]");
		commands.put("delete", "Deletes a badge [Admin Only]");
		commands.put("reload", "Reload Badges from the Config [Admin Only]");
		commands.put("rerun", "Debug Command [Admin Only]");
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
			if (legacyBadge == null) {
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}

			if (permissionCheck(SCSettings.PERMISSION_BASE + legacyBadge.name)
					|| permissionCheck(SCSettings.GROUPSHARE_BASE + legacyBadge.name)
					|| permissionCheck(SCSettings.GROUP_BASE + legacyBadge.name)) {
				Player player;


				try {
					player = getPlayer();
					sendMessage(sender, ChatColor.DARK_GREEN + "Badge Set to:"
							+ ChatColor.translateAlternateColorCodes('&', legacyBadge.badgeText));
					User user = SCSettings.getLuckPermsAPI().getUserManager().getUser(player.getUniqueId());
					SCSettings.setSuffix(user, legacyBadge.badgeText);
				} catch (SCException e) {
					e.printStackTrace();
				}

			} else {
				throw new SCException("You don't own the " + legacyBadge.name + " Badge.");
			}
		} else {
			Player player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			if (badge.canUseBadge(playerUUID)) {
				sendMessage(sender, ChatColor.DARK_GREEN + "Badge Set to:"
						+ ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()));

				User user = SCSettings.getLuckPermsAPI().getUserManager().getUser(player.getUniqueId());
				SCSettings.setSuffix(user, badge.getBadgeText());
			} else {
				sendMessage(sender, ChatColor.DARK_RED + "You don't have 'use' access to the " + badge.getName() + " Badge.");
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
		OfflinePlayer player = SuffixCommands.getInstance().getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();

		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canUseBadge(playerUUID)) {
				throw new SCException(
						playerName + " already has 'use' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addMemberUUID(playerUUID); 
			sendMessage(sender, ChatColor.GREEN + playerName + " was given 'use' access to the '" + badge.getName()
					+ "' Badge Group.");
			return;
		}

		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '" + badge.getName() + "' Badge Group.");
		}

		if (badge.canGiveBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canUseBadge(playerUUID)) {
				throw new SCException(
						playerName + " already has 'use' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addMemberUUID(playerUUID);
			sendMessage(sender, ChatColor.GREEN + playerName + " was given 'use' access to the '" + badge.getName()
					+ "' Badge Group.");
		} else {
			sendMessage(sender, 
					ChatColor.DARK_RED + "You don't have 'give' access to the '" + badge.getName() + "' Badge Group.");
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
		OfflinePlayer player = SuffixCommands.getInstance().getServer().getOfflinePlayer(playerName);
		String playerUUID = player.getUniqueId().toString();

		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canGiveBadge(playerUUID)) {
				throw new SCException(
						playerName + " already has 'give' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addLeaderUUID(playerUUID);
			sendMessage(sender, ChatColor.GREEN + playerName + " was given 'give' access to the '" + badge.getName()
					+ "' Badge Group.");
			return;
		}

		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '" + badge.getName() + "' Badge Group.");
		}

		if (badge.canGiveBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canGiveBadge(playerUUID)) {
				throw new SCException(
						playerName + " already has 'give' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addLeaderUUID(playerUUID);
			sendMessage(sender, ChatColor.GREEN + playerName + " was given 'give' access to the '" + badge.getName()
					+ "' Badge Group.");
		} else {
			sendMessage(sender,
					ChatColor.DARK_RED + "You don't have 'share' access to the '" + badge.getName() + "' Badge Group.");
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

		try {

			@SuppressWarnings("deprecation")
			OfflinePlayer player = SuffixCommands.getInstance().getServer().getOfflinePlayer(playerName);
			Player sender = getPlayer();

			String senderUUID = sender.getUniqueId().toString();
			String playerUUID = player.getUniqueId().toString();

			if (senderUUID.equals(playerUUID)) {
				throw new SCException("You cannot remove yourself from the '" + badge.getName() + "' Badge Group.");
			}
			if (badge.canShareBadge(senderUUID) && badge.canShareBadge(playerUUID)) {
				sendMessage(sender, ChatColor.DARK_GREEN + "Removed " + player + "'s 'give' access to the '" + badge.getName() + "' Badge Group");

				badge.removeLeaderUUID(playerUUID);
				User user = SCSettings.getLuckPermsAPI().getUserManager().getUser(player.getUniqueId());
				String suffix = SCSettings.getSuffix(user);
				if (suffix.equals(badge.getBadgeText())) {
					CommandSender server = Bukkit.getConsoleSender();
					Bukkit.getServer().dispatchCommand(server, "lp user " +  user.getUsername()  + " meta clear suffixes");
				}

			} else if (badge.canGiveBadge(senderUUID)) {
				if (badge.canShareBadge(playerUUID)) {
					throw new SCException("You cannot remove the owner from the '" + badge.getName() + "' Badge Group.");
				} else if (badge.canGiveBadge(playerUUID)) {
					throw new SCException("You cannot remove another Leader from the '" + badge.getName() + "' Badge Group.");
				}
				sendMessage(sender, ChatColor.DARK_GREEN + "Removed " + player + "'s 'use' access to the '" + badge.getName() + "' Badge Group");

				badge.removeMemberUUID(playerUUID);
			} else {
				sendMessage(sender, ChatColor.DARK_RED + "You don't have 'share' access to the '" + badge.getName() + "' Badge Group.");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			throw new SCException("You are the leader of the '" + badge.getName() + "' Badge Group. You cannot leave.");
		} else if (badge.canUseBadge(senderUUID)) {
			if (badge.canGiveBadge(senderUUID)) {
				badge.removeLeaderUUID(senderUUID);
			}
			badge.removeMemberUUID(senderUUID);

			User user = SCSettings.getLuckPermsAPI().getUserManager().getUser(senderUUID);
			String suffix = SCSettings.getSuffix(user);

			if (suffix.equals(badge.getBadgeText())) {
				CommandSender server = Bukkit.getServer().getConsoleSender();
				Bukkit.dispatchCommand(server, "lp user " +  user.getUsername()  + " meta clear suffixes");
			}

			sendMessage(sender, ChatColor.DARK_RED + "You have given up access to the '" + badge.getName() + "' Badge Group.");
		} else {
			sendMessage(sender, ChatColor.DARK_RED + "You already don't have access to the '" + badge.getName() + "' Badge Group.");
		}
	}

	public void list_cmd() throws SCException {
		sendHeading(sender, "List of all Legacy Badges");
		sendMessage(sender,
				ChatColor.YELLOW + "Buy Badges: " + ChatColor.DARK_GREEN + "http://buy.minetexas.com/category/572289");
		for (ConfigBadges badge : SCSettings.legacyBadges.values()) {
			if (permissionCheck(SCSettings.PERMISSION_BASE + badge.name)
					|| permissionCheck(SCSettings.GROUP_BASE + badge.name)) {
				sendMessage(sender, badge.name + ChatColor.DARK_GREEN + " [Owned]:"
						+ ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			} else {
				sendMessage(sender, badge.name + ChatColor.GRAY + " [Unowned]:"
						+ ChatColor.translateAlternateColorCodes('&', badge.badgeText));
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
				sendMessage(sender, badge.getName() + ChatColor.DARK_GREEN + " [" + status + "]:"
						+ ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()));
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
				sendMessage(sender, badge.getName() + ChatColor.DARK_GREEN + " [" + status + "]:"
						+ ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()));
			}
		}
		for (ConfigBadges badge : SCSettings.legacyBadges.values()) {
			if (permissionCheck(SCSettings.PERMISSION_BASE + badge.name)
					|| permissionCheck(SCSettings.GROUP_BASE + badge.name)) {
				hasBadges = true;
				sendMessage(sender, badge.name + ":" + ChatColor.translateAlternateColorCodes('&', badge.badgeText));
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
			sendMessage(sender, ChatColor.GREEN + "Badge removed");
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
					"lp user " +  player.getName()  + " meta clear suffixes");
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
			
			// List all players who have access to the badge group.
			sendMessage(sender, ChatColor.DARK_GREEN + "[Owner]: " + ChatColor.ITALIC
					+ Bukkit.getServer().getOfflinePlayer(UUID.fromString(badge.getOwnerUUID())).getName());

			ArrayList<String> leaders = badge.getLeaderUUIDs();
			if (!leaders.isEmpty()) {
				String leaderString = ChatColor.GREEN + "[Leaders]: " + ChatColor.ITALIC;
				for (String uuidString : leaders) {
					leaderString += Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName() + ", ";
				}

				sendMessage(sender, leaderString);
			}
			ArrayList<String> members = badge.getMemberUUIDs();
			if (!members.isEmpty()) {
				String memberString = ChatColor.WHITE + "[Members]: " + ChatColor.ITALIC;

				for (String uuidString : badge.getMemberUUIDs()) {
					memberString += Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName() + ", ";

				}
				sendMessage(sender, memberString);
			}
		} else {
			sendMessage(sender, ChatColor.DARK_RED + "You don't have 'use' access to the " + badge.getName() + " Badge.");
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
		String badgeName = args[1];
		String playerName = args[2];
		final String badgeText = args[3];
		String colorCode = args[4];
		Badge badgeFromText = null;
		
		if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
			Map<String, Badge> badges = SCSettings.badges;
			Badge badgeFromName = badges.get(badgeName);
			for (String key : badges.keySet()) {
			    Badge b = badges.get(key);
				if (b.getBadgeText().equals(badgeText)) {
					badgeFromText = b;
					break;
				}   
			}
			if (badgeFromName == null && badgeFromText == null) {
				@SuppressWarnings("deprecation")
				OfflinePlayer player = SuffixCommands.getInstance().getServer().getOfflinePlayer(playerName);
				String playerUUID = player.getUniqueId().toString();

				try {
					Badge newBadge = new Badge(badgeName, " " + badgeText, colorCode, playerUUID);
					SCLog.debug("PlayerName:" +  playerName  + "; UUID: " + playerUUID);
					try {
						newBadge.saveNow();
						SCSettings.badges.put(badgeName, newBadge);
					} catch (SQLException e) {
						throw new SCException("Badge save failed");
					}

					sendMessage(sender, ChatColor.DARK_RED + "Badge Created");
				} catch (InvalidNameException e) {
					e.printStackTrace();

					throw new SCException("Badge create failed");
				}

				
			} else {

				throw new SCException("Badge already exists");
			}
		} else {

			sendMessage(sender, ChatColor.DARK_RED + "You don't have 'create' access for Badge Groups.");
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

		newName = newName.replaceAll("�", "");
		newName = newName.replaceAll("&", "");
		badgeText = badgeText.replaceAll("�", "&");
		badgeText = badgeText.replaceAll("&k", "");
		if (newName.length() >= 16 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

			throw new SCException("Please limit your New name to 10 characters.");
		}
		if (badgeText.length() >= 20 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

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
			Badge badgeFromText = null;
		
			Map<String, Badge> badges = SCSettings.badges;
			for (String key : badges.keySet()) {
			    Badge b = badges.get(key);
				if (b.getBadgeText().equals(badgeText)) {
					badgeFromText = b;
					break;
				}   
			}
			if ((newBadge == null && badgeFromText == null) || name.equals(newName)) {
				try {
					badge.rename(newName, " " + badgeText);
					SCLog.debug("Done: senderName:" +  playerUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
					sendMessage(sender,
							ChatColor.DARK_GREEN + "'" + name + "' has been renamed to '" + newName
									+ "' with the badgeText of '"
									+ ChatColor.translateAlternateColorCodes('&', badgeText) + ChatColor.DARK_GREEN + "'");
				} catch (InvalidNameException e) {
					SCLog.exception("Badge Save failed", e);
					SCLog.debug("Error: senderName:" +  playerUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
					throw new SCException("Badge save failed, Contact an admin.");
				}
			} else {
				SCLog.debug("Error: senderName:" +  playerUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
				throw new SCException("Badge already exists");
			}
		}
	}
	
	public void setowner_cmd() throws SCException {
		if (this.args.length < 2) {
			throw new SCException("Enter a Badge Name. /badge setowner [badge] [player]");
		}
		if (this.args.length < 3) {
			throw new SCException("Enter the new Owner for the badge. Use /badge setowner [badge] [player]");
		}
		String playerName = this.args[2];
		Player player = getPlayer();
		String playerUUID = player.getUniqueId().toString();

		@SuppressWarnings("deprecation")
		OfflinePlayer newPlayer = SuffixCommands.getInstance().getServer().getOfflinePlayer(playerName);
		String newplayerUUID = newPlayer.getUniqueId().toString();
		Badge badge = (Badge)SCSettings.badges.get(this.args[1]);
		if (badge == null)
			throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
		if (badge.canShareBadge(playerUUID).booleanValue() || permissionCheck("suffixcommands.createbadges").booleanValue())
		  try {
		    badge.changeHands(newplayerUUID);
			sendMessage(sender, ChatColor.DARK_GREEN + "Changed owner of '" + badge.getName() + "' Gang to '" + newPlayer.getName());
		  } catch (SQLException e) {
		    e.printStackTrace();
		  }  
	}

	public void reload_cmd() throws SCException {
		try {
			SCSettings.reloadBadgeConfigFile();
			sendMessage(sender, "Badges reloaded");
		} catch (IOException | InvalidConfigurationException | InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}
	
	public void rerun_cmd() throws SCException {
		String senderUUID = args[1];

		args[0] = "";
		args[1] = "";
		String commandType = args[2].toLowerCase();
		
		if (commandType.equals("rename")) {
			if (args.length < 6) {
				throw new SCException("Missing Badge Display name");
			} else if (args.length > 6) {
				throw new SCException("Invalid Badge Name.");
			}
			
			String name = args[3];
			String newName = args[4];
			String badgeText = args[5];

			newName = newName.replaceAll("�", "");
			newName = newName.replaceAll("&", "");
			badgeText = badgeText.replaceAll("�", "&");
			badgeText = badgeText.replaceAll("&k", "");
			if (newName.length() >= 16 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

				SCLog.debug("Error: senderName:" +  senderUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
				throw new SCException("Please limit your New name to 10 characters.");
			}
			if (badgeText.length() >= 20 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

				SCLog.debug("Error: senderName:" +  senderUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
				throw new SCException("Please limit your displayText to 12 characters.");
			}


			Badge badge = SCSettings.badges.get(args[1]);
			if (badge == null) {
				SCLog.debug("Error: senderName:" +  senderUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
				throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization");
			}

			if (badge.canShareBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {

				Badge newBadge = SCSettings.badges.get(newName);
				Badge badgeFromText = null;
			
				Map<String, Badge> badges = SCSettings.badges;
				for (String key : badges.keySet()) {
				    Badge b = badges.get(key);
					if (b.getBadgeText().equals(badgeText)) {
						badgeFromText = b;
						break;
					}   
				}
				if ((newBadge == null && badgeFromText == null) || name.equals(newName)) {
					try {
						badge.rename(newName, " " + badgeText);
						SCLog.debug("Done: senderName:" +  senderUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
						sendMessage(sender,
								ChatColor.DARK_GREEN + "'" + name + "' has been renamed to '" + newName
										+ "' with the badgeText of '"
										+ ChatColor.translateAlternateColorCodes('&', badgeText) + ChatColor.DARK_GREEN + "'");
					} catch (InvalidNameException e) {
						SCLog.debug("Error: senderName:" +  senderUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
						throw new SCException("Badge save failed, Contact an admin.");
					}
				} else {
					SCLog.debug("Error: senderName:" +  senderUUID  + "; cmd: rename " + name + " " + newName + " " + badgeText);
					throw new SCException("Badge already exists");
				}
			}
			return;
		}
		
		String playerUUID = args[4];
		StringBuilder builder = new StringBuilder();
		builder.append("badge");
		for(String s : args) {
			if (s.length() >= 1) {
				builder.append(" " + s);
			}
		}
		

		Badge badge = SCSettings.badges.get(args[3]);
		if (badge == null) {
			throw new SCException("Badge Not Found");
		}

		
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("Same Player");
		}

		String command = builder.toString();
		if (commandType.equals("give")) {
			if (badge.canGiveBadge(senderUUID)) {
				if (badge.canUseBadge(playerUUID)) {
					throw new SCException(playerUUID + " already has 'use' access to the '" + badge.getName() + "' Badge Group.");
				}
				badge.addMemberUUID(playerUUID);
				SCLog.debug("Done: senderName:" +  senderUUID  + "; cmd: " + command);

			} else {
				throw new SCException("Player missing permission");
			}
			return;
		}
		if (commandType.equals("take")) {
			if (badge.canShareBadge(senderUUID) && badge.canGiveBadge(playerUUID)) {
				sendMessage(sender, ChatColor.DARK_GREEN + "Removed " + playerUUID + " 'give' access to the '"
						+ badge.getName() + "' Badge Group");

				badge.removeLeaderUUID(playerUUID);
				SCLog.debug("Done: senderName:" +  senderUUID  + "; cmd: " + command);

			} else if (badge.canGiveBadge(senderUUID)) {
				if (badge.canShareBadge(playerUUID)) {
					throw new SCException("You cannot remove the owner from the '" + badge.getName() + "' Badge Group.");
				} else if (badge.canGiveBadge(playerUUID)) {
					throw new SCException("You cannot remove another Leader from the '" + badge.getName() + "' Badge Group.");
				}

				badge.removeMemberUUID(playerUUID);
				SCLog.debug("Done: senderName:" +  senderUUID  + "; cmd: " + command);
			} else {
				throw new SCException("Player missing permission");
			}
			return;
		}

		if (commandType.equals("share")) {
			if (badge.canGiveBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
				if (badge.canGiveBadge(playerUUID)) {
					throw new SCException(
							playerUUID + " already has 'give' access to the '" + badge.getName() + "' Badge Group.");
				}

				badge.addLeaderUUID(playerUUID);
				SCLog.debug("Done: senderName:" +  senderUUID  + "; cmd: " + command);
			} else {
				throw new SCException("Player missing permission");
			}
			return;
		}
		
		SCLog.debug("Failed: uuid:" +  senderUUID  + "; cmd: " + command);
		
	}

	@Override
	public void doDefaultAction() {
		// TODO Auto-generated method stub
		showBasicHelp();
	}

	public void delete_cmd() throws SCException {
		try {
			if (!permissionCheck(SCSettings.BADGE))
	            throw new SCException("You must be an Admin to do this"); 
	        if (this.args.length != 2)
	            throw new SCException("Enter a group Badge Name"); 
	        Badge badge = (Badge)SCSettings.badges.get(this.args[1]);
	        if (badge == null)
	        	throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
	        badge.delete();
			sendMessage(sender, ChatColor.DARK_RED + "Deleted '" + badge.getName() + "' Badge.");
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
	}

	@Override
	public void showHelp() {
		if (permissionCheck(SCSettings.BADGE)) {
			return;
		}

		showBasicHelp();

	}

	public void showBasicHelp() {
		sendHeading(sender, displayName);
		for (String c : commands.keySet()) {
			String info = commands.get(c);

			info = info.replace("[", ChatColor.YELLOW + "[");
			info = info.replace("]", "]" + ChatColor.GRAY);
			info = info.replace("(", ChatColor.YELLOW + "(");
			info = info.replace(")", ")" + ChatColor.GRAY);
			
			sendMessage(sender, ChatColor.LIGHT_PURPLE + command + " " + c + ChatColor.GRAY + " " + info);
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
