package com.minetexas.suffixcommands;

import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import com.minetexas.suffixcommands.commands.BadgeCommand;
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
	}
 
	@Override
	public void onDisable() {
		SCLog.info("onDisable has been invoked!");
	}
}
