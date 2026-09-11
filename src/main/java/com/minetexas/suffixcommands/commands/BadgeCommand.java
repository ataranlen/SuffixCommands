package com.minetexas.suffixcommands.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
	private static final long RENAME_CONFIRM_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60);
	private static final Map<String, PendingRename> PENDING_RENAMES = new HashMap<String, PendingRename>();

	private static final class PendingRename {
		private final String name;
		private final String newName;
		private final String badgeText;
		private final long expiresAtMillis;

		private PendingRename(String name, String newName, String badgeText, long expiresAtMillis) {
			this.name = name;
			this.newName = newName;
			this.badgeText = badgeText;
			this.expiresAtMillis = expiresAtMillis;
		}

		private boolean isExpired() {
			return System.currentTimeMillis() > this.expiresAtMillis;
		}
	}

	public static void queueRename(Player player, String name, String newName, String badgeText) {
		String playerUUID = player.getUniqueId().toString();
		PendingRename pending = new PendingRename(name, newName, badgeText, System.currentTimeMillis() + RENAME_CONFIRM_TIMEOUT_MS);
		PENDING_RENAMES.put(playerUUID, pending);
		sendPendingRenamePrompt(player, name, newName, badgeText);
		Bukkit.getScheduler().runTaskLater(SCSettings.plugin, new Runnable() {
			@Override
			public void run() {
				PendingRename timedOut = PENDING_RENAMES.remove(playerUUID);
				if (timedOut == null) {
					return;
				}
				Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(playerUUID));
				if (onlinePlayer != null && onlinePlayer.isOnline()) {
					sendMessage(onlinePlayer, SCColor.Red + "Rename timed out for '" + timedOut.name + "'. No changes were made.");
				}
			}
		}, RENAME_CONFIRM_TIMEOUT_MS / 50L);
	}

	public static void sendPendingRenamePrompt(Player player, String name, String newName, String badgeText) {
		TextComponent header = new TextComponent(getPendingRenamePreview(name, newName, badgeText));
		header.setColor(net.md_5.bungee.api.ChatColor.GREEN);

		TextComponent confirm = new TextComponent("[Confirm]");
		confirm.setColor(net.md_5.bungee.api.ChatColor.GREEN);
		confirm.setBold(true);
		confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge rename confirm"));
		confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new ComponentBuilder("Confirm this rename").color(net.md_5.bungee.api.ChatColor.GREEN).create()));

		TextComponent cancel = new TextComponent("[Cancel]");
		cancel.setColor(net.md_5.bungee.api.ChatColor.RED);
		cancel.setBold(true);
		cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge rename cancel"));
		cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new ComponentBuilder("Cancel this rename").color(net.md_5.bungee.api.ChatColor.RED).create()));

		TextComponent lineBreak = new TextComponent("\n");
		player.spigot().sendMessage(header, lineBreak, confirm, new TextComponent(" "), cancel);
	}

	public static String getPendingRenamePreview(String name, String newName, String badgeText) {
		String formattedBadgeText = ChatColor.translateAlternateColorCodes('&', badgeText);
		StringBuilder preview = new StringBuilder();
		preview.append(SCColor.Green).append("Rename pending for '").append(name).append("'");
		if (!name.equals(newName)) {
			preview.append(" to '").append(newName).append("'");
		}
		preview.append(" with badge text '")
				.append(formattedBadgeText)
				.append(SCColor.Green)
				.append("'. Click or Type /badge rename confirm within 60 seconds or /badge rename cancel to cancel.");
		return preview.toString();
	}

	private static String getCancelRenameMessage(PendingRename pending) {
		StringBuilder message = new StringBuilder();
		message.append("Cancelled the pending rename of '").append(pending.name).append("'");
		if (!pending.name.equals(pending.newName)) {
			message.append(" to '").append(pending.newName).append("'");
		}
		message.append(".");
		return message.toString();
	}

	public static void cancelPendingRename(Player player, CommandSender replyTo) {
		String playerUUID = player.getUniqueId().toString();
		PendingRename pending = PENDING_RENAMES.remove(playerUUID);
		if (pending == null) {
			sendMessage(replyTo, SCColor.Red + "You do not have a pending badge rename to cancel.");
			return;
		}
		sendMessage(replyTo, SCColor.Green + getCancelRenameMessage(pending));
	}

	public static void confirmPendingRename(Player player, CommandSender replyTo) throws SCException {
		String playerUUID = player.getUniqueId().toString();
		PendingRename pending = PENDING_RENAMES.get(playerUUID);
		if (pending == null) {
			sendMessage(replyTo, SCColor.Red + "You do not have a pending badge rename to confirm.");
			return;
		}
		if (pending.isExpired()) {
			PENDING_RENAMES.remove(playerUUID);
			sendMessage(replyTo, SCColor.Red + "Your pending badge rename expired after 60 seconds and was cancelled.");
			sendMessage(replyTo, SCColor.Red + "Rename timed out for '" + pending.name + "'. No changes were made.");
			return;
		}
		PENDING_RENAMES.remove(playerUUID);
		BadgeCommand command = new BadgeCommand();
		command.sender = replyTo;
		command.args = new String[] { "rename", pending.name, pending.newName, pending.badgeText };
		command.executeRename(player, pending.name, pending.newName, pending.badgeText, replyTo);
	}

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
		commands.put("rename", "Queue a badge rename for confirmation. Usage: /badge rename [badge] [newname] [badgeText] (then /badge rename confirm or /badge rename cancel)");
		commands.put("delete", "Deletes a badge [Admin Only]");
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
			if (legacyBadge == null) {
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}

			if (permissionCheck(SCSettings.PERMISSION_BASE + legacyBadge.name)
					|| permissionCheck(SCSettings.GROUPSHARE_BASE + legacyBadge.name)
					|| permissionCheck(SCSettings.GROUP_BASE + legacyBadge.name)) {
				Player player;
				try {
					player = getPlayer();
					sendMessage(sender, SCColor.Green + "Badge Set to:"
							+ ChatColor.translateAlternateColorCodes('&', legacyBadge.badgeText));
					String command = "pex user " + player.getName() + " suffix \"" + legacyBadge.badgeText + "\"";
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
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
				sendMessage(sender, SCColor.Green + "Badge Set to:"
						+ ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()));
				String command = "pex user " + player.getName() + " suffix \"" + badge.getBadgeText() + "\"";
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			} else {
				sendMessage(sender, SCColor.Red + "You don't have 'use' access to the " + badge.getName() + " Badge.");
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
				throw new SCException(
						playerName + " already has 'use' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addMemberUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen + playerName + " was given 'use' access to the '" + badge.getName()
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
			sendMessage(sender, SCColor.LightGreen + playerName + " was given 'use' access to the '" + badge.getName()
					+ "' Badge Group.");
		} else {
			sendMessage(sender,
					SCColor.Red + "You don't have 'give' access to the '" + badge.getName() + "' Badge Group.");
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
				throw new SCException(playerName + " already has 'give' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addLeaderUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen + playerName + " was given 'give' access to the '" + badge.getName()
					+ "' Badge Group.");
			return;
		}

		Player sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		if (senderUUID.equals(playerUUID)) {
			throw new SCException("You cannot add yourself to the '" + badge.getName() + "' Badge Group.");
		}

		if (badge.canShareBadge(senderUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (badge.canGiveBadge(playerUUID)) {
				throw new SCException(
						playerName + " already has 'give' access to the '" + badge.getName() + "' Badge Group.");
			}

			badge.addLeaderUUID(playerUUID);
			sendMessage(sender, SCColor.LightGreen + playerName + " was given 'give' access to the '" + badge.getName()
					+ "' Badge Group.");
		} else {
			throw new SCException("You don't have 'share' access to the '" + badge.getName() + "' Badge Group.");
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
			throw new SCException("You cannot remove yourself from the '" + badge.getName() + "' Badge Group.");
		}

		if (badge.canShareBadge(senderUUID) && badge.canGiveBadge(playerUUID)) {
			sendMessage(sender, SCColor.Green + "Removed " + player.getName() + "'s 'give' access to the '"
					+ badge.getName() + "' Badge Group");

			badge.removeLeaderUUID(playerUUID);

		} else if (badge.canGiveBadge(senderUUID)) {
			if (badge.canShareBadge(playerUUID)) {
				throw new SCException("You cannot remove the owner from the '" + badge.getName() + "' Badge Group.");
			} else if (badge.canGiveBadge(playerUUID)) {
				throw new SCException("You cannot remove another Leader from the '" + badge.getName() + "' Badge Group.");
			}
			sendMessage(sender, SCColor.Green + "Removed " + player.getName() + "'s 'use' access to the '"
					+ badge.getName() + "' Badge Group");

			badge.removeMemberUUID(playerUUID);
		} else {
			throw new SCException("You don't have 'share' access to the '" + badge.getName() + "' Badge Group.");
		}

		PermissionUser user = PermissionsEx.getUser(playerName);

		if (user != null) {
			String suffix = user.getSuffix();
			if (suffix.equals(badge.getBadgeText())) {
				String clearSuffix = "pex user " + playerName + " suffix \"\"";
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), clearSuffix);
			}
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

			PermissionUser user = PermissionsEx.getUser(sender);

			if (user != null) {
				String suffix = user.getSuffix();
				if (suffix.equals(badge.getBadgeText())) {
					String clearSuffix = "pex user " + sender.getName() + " suffix \"\"";
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), clearSuffix);
				}
			}

			sendMessage(sender, SCColor.Red + "You have given up access to the '" + badge.getName() + "' Badge Group.");
		} else {
			sendMessage(sender,
					SCColor.Red + "You already don't have access to the '" + badge.getName() + "' Badge Group.");
		}
	}

	public void list_cmd() throws SCException {
		sendHeading(sender, "List of all Legacy Badges");
		sendMessage(sender,
				SCColor.Yellow + "Buy Badges: " + SCColor.Green + "http://buy.minetexas.com/category/572289");
		for (ConfigBadges badge : SCSettings.legacyBadges.values()) {
			if (permissionCheck(SCSettings.PERMISSION_BASE + badge.name)
					|| permissionCheck(SCSettings.GROUP_BASE + badge.name)) {
				sendMessage(sender, badge.name + SCColor.Green + " [Owned]:"
						+ ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			} else {
				sendMessage(sender, badge.name + SCColor.LightGray + " [Unowned]:"
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
				sendMessage(sender, badge.getName() + SCColor.Green + " [" + status + "]:"
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
				sendMessage(sender, badge.getName() + SCColor.Green + " [" + status + "]:"
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
			sendMessage(sender, SCColor.LightGreen + "Badge removed");
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
					"pex user " + player.getName() + " suffix \"\"");
		} catch (SCException e) {
			e.printStackTrace();
		}
	}

	private boolean canShowTakeAction(Badge badge, String viewerUUID, String targetUUID) {
		if (viewerUUID.equals(targetUUID)) { // If user is self, no kick
			return false;
		}
		if (badge.canShareBadge(viewerUUID)) { // If user is owner, can kick anyone
			return true;
		}
		if (!badge.canGiveBadge(viewerUUID)) { // If user is not a leader, cannot kick anyone
			return false;
		}
		return badge.isMember(targetUUID) && !badge.canShareBadge(targetUUID);
	}

	private void sendClickableTakeAction(Player player, String badgeName, String memberName, boolean allow) {
		TextComponent member = new TextComponent(memberName);
		member.setColor(net.md_5.bungee.api.ChatColor.WHITE);
		if (!allow) {
			player.spigot().sendMessage(member);
			return;
		}
		TextComponent take = new TextComponent(" [Kick]");
		take.setColor(net.md_5.bungee.api.ChatColor.RED);
		take.setBold(true);
		take.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
				"/badge take " + badgeName + " " + memberName));
		take.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new ComponentBuilder("Remove " + memberName + " from the badge group").color(net.md_5.bungee.api.ChatColor.RED).create()));
		player.spigot().sendMessage(member, take);
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
			TextComponent ownerLabel = new TextComponent("[Owner]: ");
			ownerLabel.setColor(net.md_5.bungee.api.ChatColor.GREEN);
			TextComponent ownerName = new TextComponent(Bukkit.getServer().getOfflinePlayer(UUID.fromString(badge.getOwnerUUID())).getName());
			ownerName.setColor(net.md_5.bungee.api.ChatColor.ITALIC);
			player.spigot().sendMessage(ownerLabel, ownerName);

			ArrayList<String> leaders = badge.getLeaderUUIDs();
			if (!leaders.isEmpty()) {
				TextComponent leadersLabel = new TextComponent("[Leaders]: ");
				leadersLabel.setColor(net.md_5.bungee.api.ChatColor.GREEN);
				player.spigot().sendMessage(leadersLabel);
				for (String uuidString : leaders) {
					String memberName = Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName();
					if (memberName != null) {
						sendClickableTakeAction(player, badge.getName(), memberName,
							canShowTakeAction(badge, playerUUID, uuidString));
					}
				}
			}
			ArrayList<String> members = badge.getMemberUUIDs();
			if (!members.isEmpty()) {
				TextComponent membersLabel = new TextComponent("[Members]: ");
				membersLabel.setColor(net.md_5.bungee.api.ChatColor.GREEN);
				player.spigot().sendMessage(membersLabel);
				for (String uuidString : badge.getMemberUUIDs()) {
					String memberName = Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidString)).getName();
					if (memberName != null) {
						sendClickableTakeAction(player, badge.getName(), memberName,
							canShowTakeAction(badge, playerUUID, uuidString));
					}
				}
			}
		} else {
			sendMessage(sender, SCColor.Red + "You don't have 'use' access to the " + badge.getName() + " Badge.");
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
				OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
				String playerUUID = player.getUniqueId().toString();

				try {
					Badge newBadge = new Badge(badgeName, " "+badgeText, colorCode, playerUUID);
					SCLog.debug("PlayerName:"+ playerName +"; UUID: "+playerUUID);
					try {
						newBadge.saveNow();
						SCSettings.badges.put(badgeName, newBadge);
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
		if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
			confirmPendingRename(getPlayer(), sender);
			return;
		}
		if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
			cancelPendingRename(getPlayer(), sender);
			return;
		}
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
		this.validateRenameInputs(name, newName, badgeText);

		Player player = getPlayer();
		queueRename(player, name, newName, badgeText);
	}

	private void executeRename(Player player, String name, String newName, String badgeText, CommandSender replyTo) throws SCException {
		String playerUUID = player.getUniqueId().toString();
		newName = newName.replaceAll("�", "");
		newName = newName.replaceAll("&", "");
		badgeText = badgeText.replaceAll("�", "&");
		badgeText = badgeText.replaceAll("&k", "");
		badgeText = badgeText.replaceAll("&K", "");
		this.validateRenameInputs(name, newName, badgeText);

		Badge badge = SCSettings.badges.get(name);
		if (badge == null) {
			throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization");
		}

		if (badge.canShareBadge(playerUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			if (!permissionCheck(SCSettings.PERMISSION_CREATE) && !badge.canRenameNow(SCSettings.getIntegerBase("rename_timeout_days", 7))) {
				throw new SCException("This badge can only be renamed once every " + SCSettings.getIntegerBase("rename_timeout_days", 7)
							+ " day(s). You can rename it again on " + getNextRenameTime(badge, SCSettings.getIntegerBase("rename_timeout_days", 7)) + ".");
			}

			Badge newBadge = SCSettings.badges.get(newName);
			Badge badgeFromText = null;

			String cleanBadgeText = badgeText.replaceAll("�", "&");
			String visibleNewName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cleanBadgeText));
		
			Map<String, Badge> badges = SCSettings.badges;
			for (String key : badges.keySet()) {
			    Badge b = badges.get(key);
				String bCleanBadgeText = b.getBadgeText().replaceAll("�", "&");
				String bVisibleName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', bCleanBadgeText));
				if (bVisibleName.equals(visibleNewName)) {
					badgeFromText = b;
					break;
				}
			}
			if ((newBadge == null && badgeFromText == null) || name.equals(newName)) {
				try {
					badge.rename(newName, " " + badgeText);
					SCLog.debug("Done: senderName:"+ playerUUID +"; cmd: rename " + name + " " + newName + " " + badgeText);
					String updatedBadgeText = ChatColor.translateAlternateColorCodes('&', badgeText);
					sendMessage(replyTo,
							SCColor.Green + "'" + name + "' has been renamed to '" + newName
									+ "' with the badgeText of '"
									+ updatedBadgeText + SCColor.Green + "'");
				} catch (InvalidNameException e) {
					SCLog.exception("Badge Save failed", e);
					SCLog.debug("Error: senderName:"+ playerUUID +"; cmd: rename " + name + " " + newName + " " + badgeText);
					throw new SCException("Badge save failed, Contact an admin.");
				}
			} else {
				SCLog.debug("Error: senderName:"+ playerUUID +"; cmd: rename " + name + " " + newName + " " + badgeText);
				throw new SCException("Badge already exists");
			}
		}
	}

	private void validateRenameInputs(String name, String newName, String badgeText) throws SCException {
		String cleanNewName = newName.replaceAll("�", "");
		cleanNewName = cleanNewName.replaceAll("&", "");
		String cleanBadgeText = badgeText.replaceAll("�", "&");
		cleanBadgeText = cleanBadgeText.replaceAll("&k", "");
		cleanBadgeText = cleanBadgeText.replaceAll("&K", "");
		int fullNewNameLength = newName.codePointCount(0, newName.length());
		if (fullNewNameLength >= 64) {
			throw new SCException("Please limit your new name to 64 characters. (Including color codes) Your new name '" + newName + "' is currently " + fullNewNameLength + " characters long.");
		}
		int cleanNewNameLength = cleanNewName.codePointCount(0, cleanNewName.length());
		if (cleanNewNameLength > 12 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {
			throw new SCException("Please limit your new name to 12 characters.");
		}
		String visibleNewName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cleanBadgeText));
		int visibleNewNameLength = visibleNewName.codePointCount(0, visibleNewName.length());
		if (visibleNewNameLength > 12 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {
			throw new SCException("Please limit your badge text to 12 characters (excluding color codes). Your badge text '" + visibleNewName + "' is currently " + visibleNewNameLength + " characters long.");
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
		OfflinePlayer newPlayer = SCSettings.plugin.getServer().getOfflinePlayer(playerName);
		String newplayerUUID = newPlayer.getUniqueId().toString();
		Badge badge = (Badge)SCSettings.badges.get(this.args[1]);
		if (badge == null)
			throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
		if (badge.canShareBadge(playerUUID).booleanValue() || permissionCheck(SCSettings.PERMISSION_CREATE).booleanValue())
		  try {
		    badge.changeHands(newplayerUUID);
			sendMessage(sender, SCColor.Green+"Changed owner of '"+badge.getName()+"' Gang to '" + newPlayer.getName());
		  } catch (SQLException e) {
		    e.printStackTrace();
		  }  
	}

	public void reload_cmd() throws SCException {
		try {
			if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
				throw new SCException("You must be an Admin to do this"); 
			}

			SCSettings.reloadBadgeConfigFile();
			sendMessage(sender, "Badges reloaded");
		} catch (IOException | InvalidConfigurationException | InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}
	
	private String getNextRenameTime(Badge badge, int timeoutDays) {
		if (badge.getLastRename() == null) {
			return "immediately";
		}
		Date nextTime = new Date(badge.getLastRename().getTime() + ((long) timeoutDays) * 24L * 60L * 60L * 1000L);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(nextTime);
	}

	@Override
	public void doDefaultAction() {
		// TODO Auto-generated method stub
		showBasicHelp();
	}

	public void delete_cmd() throws SCException {
		try {
			if (!permissionCheck(SCSettings.PERMISSION_CREATE))
	            throw new SCException("You must be an Admin to do this"); 
	        if (this.args.length != 2)
	            throw new SCException("Enter a group Badge Name"); 
	        Badge badge = (Badge)SCSettings.badges.get(this.args[1]);
	        if (badge == null)
	        	throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
	        badge.delete();
			sendMessage(sender, SCColor.Red+"Deleted '"+badge.getName()+"' Badge.");
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

			info = info.replace("[", SCColor.Yellow + "[");
			info = info.replace("]", "]" + SCColor.LightGray);
			info = info.replace("(", SCColor.Yellow + "(");
			info = info.replace(")", ")" + SCColor.LightGray);

			sendMessage(sender, SCColor.LightPurple + command + " " + c + SCColor.LightGray + " " + info);
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
