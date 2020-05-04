package com.minetexas.suffixcommands.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.degoos.wetsponge.WetSponge;
import com.degoos.wetsponge.command.WSCommandSource;
import com.degoos.wetsponge.entity.living.player.WSPlayer;
import com.degoos.wetsponge.exception.player.WSPlayerNotFoundException;
import com.degoos.wetsponge.text.WSText;
import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

import net.luckperms.api.model.user.User;

public class BadgeCommand extends CommandBase {

	public BadgeCommand() {
		super("badge", "Manage Your Badges. Badge names are case sensitive.");
	}

	@Override
	public List<String> sendTab(WSCommandSource arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		List<String> keys = new ArrayList<>(commands.keySet());
		return keys;
	}

	@Override
	public void init() {
		command = "/badge";
		displayName = "Manage Your Badges. Badge names are case sensitive.";
		
		commands.put("set", "Change your badge to one you own. Usage: /badge set [name]");
		commands.put("give", "Grant a player access to a group badge. Usage: /badge give [name] [player]");
		commands.put("take", "Remove a player's access to a group badge. Usage: /badge take [name] [player]");
		commands.put("share", "Grant a player access to give a group badge. Usage: /badge share [name] [player]");
		commands.put("leave", "Leave a group badge");
		commands.put("remove", "Remove your current badge");
		commands.put("owned", "List all your owned badges");
		commands.put("group", "List all your group badges");
		commands.put("list", "List all possible badges");
		commands.put("members", "List all members of the named badge group");
		commands.put("create", "Create a new badge group. [Admin Only] Usage: /badge create [name] [owner] [badgeText] [Chat Color Code]");
		commands.put("rename", "Rename a badge group. Usage: /badge rename [name] [newname] [badgeText]");
		commands.put("setowner", "Give ownership of the badge to someone else. [Permanent]");
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
			if (legacyBadge == null)
			{
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}
			
			if (permissionCheck(SCSettings.PERMISSION_BASE+legacyBadge.name)
					|| permissionCheck(SCSettings.GROUPSHARE_BASE+legacyBadge.name)
					|| permissionCheck(SCSettings.GROUP_BASE+legacyBadge.name)) {
				WSPlayer player;
				try {
					player = getPlayer();
					player.sendMessage(WSText.of(SCColor.Green+"Badge Set to:"+legacyBadge.badgeText).toBuilder().translateColors().build());
					User user = SCSettings.luckPermsAPI.getUserManager().getUser(player.getUniqueId());
					SCSettings.setSuffix(user, legacyBadge.badgeText);
				} catch (SCException e) {
					e.printStackTrace();
				}
				
			} else {
				throw new SCException("You don't own the "+legacyBadge.name+" Badge.");
			}
		} else {
			WSPlayer player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			if (badge.canUseBadge(playerUUID)) {
				player.sendMessage(WSText.of(SCColor.Green+"Badge Set to:"+badge.getBadgeText()).toBuilder().translateColors().build());
				User user = SCSettings.luckPermsAPI.getUserManager().getUser(player.getUniqueId());
				SCSettings.setSuffix(user, badge.getBadgeText());
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
		
		try {
			WSPlayer player = WetSponge.getServer().getPlayer(playerName).orElseThrow(WSPlayerNotFoundException::new);
			String playerUUID = player.getUniqueId().toString();
			if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
				if (badge.canUseBadge(playerUUID)) {
					throw new SCException(playerName+" already has 'use' access to the '"+badge.getName()+"' Badge Group.");
				}

				badge.addMemberUUID(playerUUID);
				sendMessage(sender, SCColor.LightGreen+playerName+" was given 'use' access to the '"+badge.getName()+"' Badge Group.");
				return;
			}
			
			WSPlayer sender = getPlayer();
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
		} catch (WSPlayerNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
		try {
			WSPlayer player = WetSponge.getServer().getPlayer(playerName).orElseThrow(WSPlayerNotFoundException::new);
			String playerUUID = player.getUniqueId().toString();
			
			if (permissionCheck(SCSettings.PERMISSION_CREATE)) {
				if (badge.canGiveBadge(playerUUID)) {
					throw new SCException(playerName+" already has 'give' access to the '"+badge.getName()+"' Badge Group.");
				}

				badge.addLeaderUUID(playerUUID);
				sendMessage(sender, SCColor.LightGreen+playerName+" was given 'give' access to the '"+badge.getName()+"' Badge Group.");
				return;
			}
			
			WSPlayer sender = getPlayer();
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
		} catch (WSPlayerNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			WSPlayer player = WetSponge.getServer().getPlayer(playerName).orElseThrow(WSPlayerNotFoundException::new);
			WSPlayer sender = getPlayer();
			String senderUUID = sender.getUniqueId().toString();
			String playerUUID = player.getUniqueId().toString();

			if (senderUUID.equals(playerUUID)) {
				throw new SCException("You cannot remove yourself from the '"+badge.getName()+"' Badge Group.");
			}
			if (badge.canShareBadge(senderUUID) && badge.canShareBadge(playerUUID)) {
				sendMessage(sender, SCColor.Green+"Removed "+player+"'s 'give' access to the '"+badge.getName()+"' Badge Group");

				badge.removeLeaderUUID(playerUUID);
				User user = SCSettings.luckPermsAPI.getUserManager().getUser(player.getUniqueId());
				String suffix = SCSettings.getSuffix(user);
				if (suffix.equals(badge.getBadgeText())) {
					WSCommandSource server = WetSponge.getServer().getConsole();
					server.performCommand("lp user "+ user.getUsername() +" meta clear suffixes");
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
		} catch (WSPlayerNotFoundException e) {
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
		
		WSPlayer sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		
		if (badge.canShareBadge(senderUUID)) {
			throw new SCException("You are the leader of the '"+badge.getName()+"' Badge Group. You cannot leave.");
		} else if (badge.canUseBadge(senderUUID)) {
			if (badge.canGiveBadge(senderUUID)) {
				badge.removeLeaderUUID(senderUUID);
			}
			badge.removeMemberUUID(senderUUID);

			User user = SCSettings.luckPermsAPI.getUserManager().getUser(senderUUID);
			String suffix = SCSettings.getSuffix(user);
			if (suffix.equals(badge.getBadgeText())) {
				WSCommandSource server = WetSponge.getServer().getConsole();
				server.performCommand("lp user "+ user.getUsername() +" meta clear suffixes");
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
				sendMessage(sender, WSText.of(badge.name+SCColor.Green+" [Owned]:"+badge.badgeText).toBuilder().translateColors().build());
			} else {
				sendMessage(sender, WSText.of(badge.name+SCColor.LightGray+" [Unowned]:"+badge.badgeText).toBuilder().translateColors().build());
			}
		}
	}
	
	public void group_cmd() throws SCException {
		Boolean hasBadges = false;
		sendHeading(sender, "List of owned Group Badges");
		WSPlayer sender = getPlayer();
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
				sendMessage(sender, WSText.of(badge.getName()+SCColor.Green+" ["+status+"]:"+badge.getBadgeText()).toBuilder().translateColors().build());
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
		WSPlayer sender = getPlayer();
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
				sendMessage(sender, WSText.of(badge.getName()+SCColor.Green+" ["+status+"]:"+badge.getBadgeText()).toBuilder().translateColors().build());
			} 
		}
		for (ConfigBadges badge : SCSettings.legacyBadges.values())
		{
			if (permissionCheck(SCSettings.PERMISSION_BASE+badge.name) || permissionCheck(SCSettings.GROUP_BASE+badge.name))
			{
				hasBadges = true;
				sendMessage(sender, WSText.of(badge.name+":"+badge.badgeText).toBuilder().translateColors().build());
			}
		}
		
		if (!hasBadges) {
			sendMessage(sender, "You don't own any badges");
		}
	}
	
	public void remove_cmd() throws SCException {		

		WSPlayer player;
		try {
			player = getPlayer();
			sendMessage(sender, SCColor.LightGreen+"Badge removed");
			WSCommandSource server = WetSponge.getServer().getConsole();
			server.performCommand("lp user "+ player.getName() +" meta clear suffixes");
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

		WSPlayer player = getPlayer();
		String playerUUID = player.getUniqueId().toString();
		if (badge.canUseBadge(playerUUID) || permissionCheck(SCSettings.PERMISSION_CREATE)) {
			//List all players who have access to the badge group.
			try {
				WSPlayer ownerPlayer = WetSponge.getServer().getPlayer(UUID.fromString(badge.getOwnerUUID())).orElseThrow(WSPlayerNotFoundException::new);
				sendMessage(sender, SCColor.Green+"[Owner]: "+SCColor.ITALIC+ownerPlayer.getName());
			} catch (WSPlayerNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			
			ArrayList<String> leaders = badge.getLeaderUUIDs();
			if (!leaders.isEmpty()) {
				String leaderString = SCColor.LightGreen+"[Leaders]: "+SCColor.ITALIC;
				for (String uuidString : leaders) {
					try {
						WSPlayer leaderPlayer = WetSponge.getServer().getPlayer(UUID.fromString(uuidString)).orElseThrow(WSPlayerNotFoundException::new);
						leaderString += leaderPlayer.getName()+", "; 
					} catch (WSPlayerNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}

				sendMessage(sender, leaderString);
			}
			ArrayList<String> members = badge.getMemberUUIDs();
			if (!members.isEmpty()) {
				String memberString = SCColor.White+"[Members]: "+SCColor.ITALIC;

				for (String uuidString : badge.getMemberUUIDs()) {
					try {
						WSPlayer memberPlayer = WetSponge.getServer().getPlayer(UUID.fromString(uuidString)).orElseThrow(WSPlayerNotFoundException::new);
						memberString += memberPlayer.getName()+", ";
					} catch (WSPlayerNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  
					
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
				try {
					WSPlayer player = WetSponge.getServer().getPlayer(playerName).orElseThrow(WSPlayerNotFoundException::new);
					String playerUUID = player.getUniqueId().toString();
					Badge newBadge = new Badge(name, " "+badgeText, colorCode, playerUUID);
					SCLog.debug("PlayerName:"+ playerName +"; UUID: "+playerUUID);
					try {
						newBadge.saveNow();
						SCSettings.badges.put(name, newBadge);
					} catch (SQLException e) {
						throw new SCException("Badge save failed");
					}

					sendMessage(sender, SCColor.Red+"Badge Created");
				} catch (InvalidNameException | WSPlayerNotFoundException e) {
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
	
	public void setowner_cmd() throws SCException {
		if (this.args.length < 2) {
			throw new SCException("Enter a Badge Name. /badge setowner [badge] [player]");
		}
		if (this.args.length < 3) {
			throw new SCException("Enter the new Owner for the badge. Use /badge setowner [badge] [player]");
		}
		String playerName = this.args[2];
		WSPlayer player = getPlayer();
		String playerUUID = player.getUniqueId().toString();
		try {
			WSPlayer newPlayer;
			newPlayer = WetSponge.getServer().getPlayer(playerName).orElseThrow(WSPlayerNotFoundException::new);
			String newplayerUUID = newPlayer.getUniqueId().toString();
			Badge badge = (Badge)SCSettings.badges.get(this.args[1]);
			if (badge == null)
				throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
			if (badge.canShareBadge(playerUUID).booleanValue() || permissionCheck("suffixcommands.createbadges").booleanValue())
			  try {
			    badge.changeHands(newplayerUUID);
			  } catch (SQLException e) {
			    e.printStackTrace();
			  }  
		} catch (WSPlayerNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
		if (newName.length() >= 16 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

			throw new SCException("Please limit your New name to 10 characters.");
		}
		if (badgeText.length() >= 20 && !permissionCheck(SCSettings.PERMISSION_CREATE)) {

			throw new SCException("Please limit your displayText to 12 characters.");
		}
		
		WSPlayer player = getPlayer();
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
					sendMessage(sender, WSText.of(SCColor.Green+"'"+name+"' has been renamed to '"+newName+"' with the badgeText of '"+badgeText+SCColor.Green+"'").toBuilder().translateColors().build());
				} catch (InvalidNameException e) {
					SCLog.exception("Badge Save failed", e);
					throw new SCException("Badge save failed, Contact an admin.");
				}
			} else {
				throw new SCException("Badge already exists");
			}
		}
	}
	
	  public void delete_cmd() throws SCException {
		  try {
		        if (permissionCheck(SCSettings.PERMISSION_CREATE))
		            throw new SCException("You must be an Admin to do this"); 
		        if (this.args.length != 2)
		            throw new SCException("Enter a group Badge Name"); 
		        Badge badge = (Badge)SCSettings.badges.get(this.args[1]);
		        if (badge == null)
		        	throw new SCException("Invalid Group Badge Name. Use exact spelling and capitalization"); 
		        badge.delete();
		    } catch (SQLException e) {
		    	e.printStackTrace();
		    } 
	}
	
	public void reload_cmd() throws SCException {
		try {
			SCSettings.reloadBadgeConfigFile();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendMessage(sender, "Badges reloaded");
	}

	@Override
	public void doDefaultAction() {
		// TODO Auto-generated method stub
		showBasicHelp();
	}

	@Override
	public void showHelp() {
		WSPlayer player;
		try {
			player = getPlayer();
		} catch (SCException e) {
			e.printStackTrace();
			return;
		}
		
		if (!player.hasPermission(SCSettings.BADGE)) {
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
		if (sender instanceof WSPlayer) {
			WSPlayer player;
			try {
				player = getPlayer();
				if (player.hasPermission(permission)) {
					return true;
				}
			} catch (SCException e) {
				e.printStackTrace();
			}			
		}
		return false;
	}

	@Override
	public void permissionCheck() {
		// TODO Auto-generated method stub
	}

}
