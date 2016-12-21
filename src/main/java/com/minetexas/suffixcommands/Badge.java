package com.minetexas.suffixcommands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.minetexas.suffixcommands.database.SQL;
import com.minetexas.suffixcommands.database.SQLObject;
import com.minetexas.suffixcommands.database.SQLUpdate;
import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.InvalidObjectException;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class Badge extends SQLObject {
	private String badgeText = "";
	private String chatColor = "";
	private String ownerUUID = "";
	private ArrayList<String> leaderUUIDs = new ArrayList<String>();
	private ArrayList<String> memberUUIDs = new ArrayList<String>();

	public static String TABLE_NAME = "BADGES";
	
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`name` VARCHAR(64) NOT NULL," + 
					"`badgeText` VARCHAR(64) NOT NULL," + 
					"`chatColor` VARCHAR(64) NOT NULL," + 
					"`ownerUUID` mediumtext," +
					"`leaderUUIDs` mediumtext DEFAULT NULL, "+
					"`memberUUIDs` mediumtext DEFAULT NULL, "+
					"UNIQUE KEY (`name`), " +
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			SCLog.info("Created "+TABLE_NAME+" table");
		} else {
			SCLog.info(TABLE_NAME+" table OK!");
		}
	}
	
	public Badge(ResultSet rs) throws InvalidObjectException, SCException, SQLException, InvalidNameException {
		this.load(rs);
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException,
			InvalidObjectException, SCException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.setBadgeText(rs.getString("badgeText"));
		this.setChatColor(rs.getString("chatColor"));
		this.setOwnerUUID(rs.getString("ownerUUID"));
		this.loadLeaderUUIDs(rs.getString("leaderUUIDs"));
		this.loadMemberUUIDs(rs.getString("memberUUIDs"));
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
		
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("name", this.getName());
		hashmap.put("badgeText", this.getBadgeText());
		hashmap.put("chatColor", this.getChatColor());
		hashmap.put("ownerUUID", this.getOwnerUUID());
		hashmap.put("leaderUUIDs", this.saveUUIDs(this.leaderUUIDs));
		hashmap.put("memberUUIDs", this.saveUUIDs(this.memberUUIDs));
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		// TODO Remove the badge from all users
		SQL.deleteNamedObject(this, TABLE_NAME);
	}
	
	public void rename(String name, String badgeText) throws SCException, InvalidNameException {
		SCSettings.badges.remove(this.getName());
		String oldName = this.getName();
		String oldBadgeText = this.getBadgeText();
		
		this.setName(name);
		this.setBadgeText(badgeText);
		
		if (!oldBadgeText.equals(badgeText)) {
			//Loop through all players who own the badge and are using the badge.
			
			for (String uuid : getAllPlayersWithAccess()) {
				OfflinePlayer player = SCSettings.plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
				String playerName = player.getName();
				if (playerName != null) {
				PermissionUser user = PermissionsEx.getUser(playerName);
					if (user != null) {
						String suffix = user.getSuffix();
						if (suffix.equals(" "+getBadgeText())) {
							String clearSuffix = "pex user "+player+" suffix \""+badgeText+"\"";
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), clearSuffix);
						}
					}
				}
			}
			
		}
		SCSettings.badges.put(name, this);
		try {
			this.saveNow();
		} catch (SQLException e) {
			SCLog.exception("Failed to rename badge: '"+oldName+"' -> '"+name+"'" , e);
			e.printStackTrace();
		}
	}
	
	public void create(ConfigBadges badge) throws SCException, InvalidNameException {
		try {
			Badge newBadge = new Badge(badge.name, badge.badgeText, badge.chatColor);
			newBadge.saveNow();
			
		} catch (SQLException e) {
			SCLog.exception("Failed to create badge from ConfigBadges object", e);
		}
	}
	
	public Badge(String name, String badgeText, String chatColor) throws InvalidNameException {
		this.setName(name);
		this.setBadgeText(badgeText);
		this.setChatColor(chatColor);
	}
	
	public Badge(String name, String badgeText, String chatColor, String ownerUUID) throws InvalidNameException {
		this.setName(name);
		this.setBadgeText(badgeText);
		this.setChatColor(chatColor);
		this.setOwnerUUID(ownerUUID);
	}
	
	public String getBadgeText() {
		return this.badgeText;
	}
	
	public void setBadgeText(String string) {
		this.badgeText = string;
	}

	public String getChatColor() {
		return chatColor;
	}

	public void setChatColor(String chatColor) {
		this.chatColor = chatColor;
	}

	public String getOwnerUUID() {
		return ownerUUID;
	}
	
	public Boolean canUseBadge(String uuid) {
		return (this.getOwnerUUID().equals(uuid) || this.isLeader(uuid) || this.isMember(uuid));
	}
	
	public Boolean canGiveBadge(String uuid) {
		return (this.getOwnerUUID().equals(uuid) || this.isLeader(uuid));
	}
	
	public Boolean canShareBadge(String uuid) {
		return this.getOwnerUUID().equals(uuid);
	}
	
	public ArrayList<String> getAllPlayersWithAccess() {
		ArrayList<String> playersWithAccess = new ArrayList<String>();
		playersWithAccess.add(this.getOwnerUUID());
		playersWithAccess.addAll(this.getLeaderUUIDs());
		playersWithAccess.addAll(this.getMemberUUIDs());
		return playersWithAccess;
	}

	public void setOwnerUUID(String ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	public ArrayList<String> getLeaderUUIDs() {
		return leaderUUIDs;
	}
	
	public Boolean isLeader(String uuid) {
		return this.leaderUUIDs.contains(uuid);
	}
	
	public void addLeaderUUID(String uuid) {
		this.leaderUUIDs.add(uuid);
		this.save();
	}
	
	public void removeLeaderUUID(String uuid) {
		this.leaderUUIDs.remove(uuid);
		this.save();
	}

	public void setLeaderUUIDs(ArrayList<String> leaderUUIDs) {
		this.leaderUUIDs = leaderUUIDs;
	}
	
	private void loadLeaderUUIDs(String leaderUUIDString) {
		if (leaderUUIDString == null || leaderUUIDString.equals("")) {
			return;
		}
		
		String[] leaders = leaderUUIDString.split(",");
		
		for (String leader : leaders) {
			this.leaderUUIDs.add(leader);
		}
	}

	public ArrayList<String> getMemberUUIDs() {
		return memberUUIDs;
	}
	
	public Boolean isMember(String uuid) {
		return this.memberUUIDs.contains(uuid);
	}
	
	public void addMemberUUID(String uuid) {
		this.memberUUIDs.add(uuid);
		this.save();
	}
	
	public void removeMemberUUID(String uuid) {
		this.memberUUIDs.remove(uuid);
		this.save();
	}
	
	public void setMemberUUIDs(ArrayList<String> memberUUIDs) {
		this.memberUUIDs = memberUUIDs;
	}
	
	private void loadMemberUUIDs(String memberUUIDString) {
		if (memberUUIDString == null || memberUUIDString.equals("")) {
			return;
		}
		
		String[] members = memberUUIDString.split(",");
		
		for (String member : members) {
			this.memberUUIDs.add(member);
		}
	}

	private Object saveUUIDs(ArrayList<String> uuidList) {
		String out = "";
		
		for (String uuid : uuidList) {
			out += uuid+",";
		}
		
		return out;
	}
}
