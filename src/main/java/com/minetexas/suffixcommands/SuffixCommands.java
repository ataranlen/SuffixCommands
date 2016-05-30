package com.minetexas.suffixcommands;

import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.minetexas.suffixcommands.commands.BadgeCommand;
import com.minetexas.suffixcommands.commands.ChatCommand;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class SuffixCommands extends JavaPlugin {
	@Override
	public void onEnable() {
		

		SCLog.init(this);
		SCLog.info("onEnable has been invoked!");
		
		try {
			SCSettings.init(this);

		} catch (IOException | InvalidConfigurationException
				| InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.getCommand("badge").setExecutor(new BadgeCommand());
		this.getCommand("chat").setExecutor(new ChatCommand());
		this.getCommand("bc").setExecutor(new ChatCommand());
	}
 
	@Override
	public void onDisable() {
		SCLog.info("onDisable has been invoked!");
	}
	
	public boolean hasPlugin(String name) {
		Plugin p;
		p = getServer().getPluginManager().getPlugin(name);
		return (p != null);
	}
}
