package com.minetexas.suffixcommands.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.SCColor;

public abstract class CommandBase implements CommandExecutor {

	protected HashMap<String, String> commands = new HashMap<String, String>();
	protected String[] args;
	protected CommandSender sender;
	
	public abstract void init();
	
	protected String command = "FIXME";
	protected String displayName = "FIXME";
	protected boolean sendUnknownToDefault = false;
	
	/* Called when no arguments are passed. */
	public abstract void doDefaultAction() throws SCException;
	
	/* Called on syntax error. */
	public abstract void showHelp();
	
	/* Called before command is executed to check permissions. */
	public abstract void permissionCheck()  throws SCException;
 
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		init();
		
		this.args = args;
		this.sender = sender;
		
		try {
			permissionCheck();
		} catch (SCException e1) {
			sendError(sender, e1.getMessage());
			return false;
		}
		
		if (args.length == 0) {
			try {
				doDefaultAction();
			} catch (SCException e) {
				sendError(sender, e.getMessage());
			}
			return false;
		}
		
		if (args[0].equalsIgnoreCase("help")) {
			showHelp();
			return true;
		}
		
		for (String c : commands.keySet()) {
			  if (c.equalsIgnoreCase(args[0])) {
					try { 
						Method method = this.getClass().getMethod(args[0].toLowerCase()+"_cmd");
						try {
							method.invoke(this);
							return true;
						} catch (IllegalAccessException | IllegalArgumentException e) {
							e.printStackTrace();
							sendError(sender, "Internal Command Exception");
						} catch (InvocationTargetException e) {
							if (e.getCause() instanceof SCException) {
								sendError(sender, e.getCause().getMessage());
							} else {
								sendError(sender, "Internal Command Exception");
								e.getCause().printStackTrace();
							}
						}

						
					} catch (NoSuchMethodException e) {
						if (sendUnknownToDefault) {
							try {
								doDefaultAction();
							} catch (SCException e1) {
								sendError(sender, e.getMessage());
							}
							return false;
						}
						sendError(sender, "Unkown Command: "+args[0]);
					}
					return true;
				}
			}
			
			if (sendUnknownToDefault) {
				try {
					doDefaultAction();
				} catch (SCException e) {
					sendError(sender, e.getMessage());
				}
				return false;
			}
			
			sendError(sender, "Unkown Command: "+args[0]);
			return false;
	}
	
	public Player getPlayer() throws SCException {
		if (sender instanceof Player) {
			return (Player)sender;
		}
		throw new SCException("You must be a player to execute this command");
	}
	
	public static void sendMessage(Object sender, String line) {
		if ((sender instanceof Player)) {
//			SCLog.debug(((Player) sender).getDisplayName()+" - "+line);
			((Player) sender).sendMessage(line);
		} else if (sender instanceof CommandSender) {
			((CommandSender) sender).sendMessage(line);
		}
	}
	public static void sendMessage(Object sender, String[] lines) {
		boolean isPlayer = false;
		if (sender instanceof Player)
			isPlayer = true;

		for (String line : lines) {
			if (isPlayer) {
				((Player) sender).sendMessage(line);
			} else {
				((CommandSender) sender).sendMessage(line);
			}
		}
	}
	
	public static String buildTitle(String title) {
		String line =   "-------------------------------------------------";
		String titleBracket = "[ " + SCColor.Yellow + title + SCColor.LightBlue + " ]";
		
		if (titleBracket.length() > line.length()) {
			return SCColor.LightBlue+"-"+titleBracket+"-";
		}
		
		int min = (line.length() / 2) - titleBracket.length() / 2;
		int max = (line.length() / 2) + titleBracket.length() / 2;
		
		String out = SCColor.LightBlue + line.substring(0, Math.max(0, min));
		out += titleBracket + line.substring(max);
		
		return out;
	}
	
	public static void sendHeading(CommandSender sender, String title) {	
		sendMessage(sender, buildTitle(title));
	}
	
	public static void sendError(Object sender, String line) {		
		sendMessage(sender, SCColor.Rose+line);
	}


}
