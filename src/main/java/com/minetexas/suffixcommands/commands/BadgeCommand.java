package com.minetexas.suffixcommands.commands;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCSettings;

public class BadgeCommand extends CommandBase {

	@Override
	public void init() {
		command = "/badge";
		displayName = "Manage Your Badges";
		
		commands.put("set", "Change your badge to one you own");
		commands.put("give", "Grant a player access to a group badge");
		commands.put("take", "Remove a player's access to a group badge");
		commands.put("remove", "Remove your current badge");
		commands.put("owned", "List all your owned badges");
		commands.put("group", "List all your group badges");
		commands.put("list", "List all possible badges");
		
		commands.put("reload", "Reload Badges from the Config [Admin Only]");
	}
	
	public void set_cmd() throws SCException {		
		if (args.length < 2) {
			throw new SCException("Enter a Badge Name");
		}
		if (args.length > 2) {
			throw new SCException("Invalid Badge Name. Use /badge set [name]");
		}
		
		ConfigBadges badge = SCSettings.badges.get(args[1]);
		if (badge == null)
		{
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		
		if (permissionCheck(SCSettings.PERMISSION_BASE+badge.name) || permissionCheck(SCSettings.GROUP_BASE+badge.name)) {
			
			Player player;
			try {
				player = getPlayer();
				sendMessage(sender, SCColor.Green+"Badge Set to:"+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
				String command = "pex user "+player.getName()+" suffix \""+badge.badgeText+"\"";
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			} catch (SCException e) {
				e.printStackTrace();
			}
			
		} else {
			sendMessage(sender, SCColor.Red+"You don't own the "+badge.name+" Badge.");
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
		
		ConfigBadges badge = SCSettings.badges.get(args[1]);
		if (badge == null)
		{
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		
		String player = args[2];
		
		if (permissionCheck(SCSettings.GROUP_BASE+badge.name)) {
			
			sendMessage(sender, SCColor.Green+"Granted '"+player+"' access to badge '"+badge.name+"': "+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			String command = "pex user "+player+" add "+SCSettings.PERMISSION_BASE+badge.name;
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

			String chatCommand = "pex user "+player+" add "+SCSettings.PERMISSION_CHAT+badge.name;
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), chatCommand);
			
		} else {
			sendMessage(sender, SCColor.Red+"You don't have access to the '"+badge.name+"' Badge Group.");
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
		
		ConfigBadges badge = SCSettings.badges.get(args[1]);
		if (badge == null)
		{
			throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
		}
		
		String player = args[2];
		
		if (permissionCheck(SCSettings.GROUP_BASE+badge.name)) {
			
			sendMessage(sender, SCColor.Green+"Removed "+player+"'s access to badge '"+badge.name+"': "+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			String command = "pex user "+player+" remove "+SCSettings.PERMISSION_BASE+badge.name;
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

			String chatCommand = "pex user "+player+" remove "+SCSettings.PERMISSION_CHAT+badge.name;
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), chatCommand);
			
		} else {
			sendMessage(sender, SCColor.Red+"You don't have access to the '"+badge.name+"' Badge Group.");
		}
	}
	
	public void list_cmd() throws SCException {
		sendHeading(sender, "List of all Badges");
		sendMessage(sender, SCColor.Yellow+"Buy Badges: "+SCColor.Green+"http://buy.minetexas.com/category/572289");
		for (ConfigBadges badge : SCSettings.badges.values())
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
		for (ConfigBadges badge : SCSettings.badges.values())
		{
			if (permissionCheck(SCSettings.GROUP_BASE+badge.name))
			{
				hasBadges = true;
				sendMessage(sender, badge.name+":"+ChatColor.translateAlternateColorCodes('&', badge.badgeText));
			}
		}
		if (!hasBadges) {
			sendMessage(sender, "You don't own any Group badges");
		}
	}
	
	public void owned_cmd() throws SCException {
		Boolean hasBadges = false;
		sendHeading(sender, "List of owned Badges");
		for (ConfigBadges badge : SCSettings.badges.values())
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
