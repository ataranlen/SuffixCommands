package com.minetexas.suffixcommands;

import com.minetexas.suffixcommands.database.SQL;
import com.minetexas.suffixcommands.database.SQLObject;
import com.minetexas.suffixcommands.database.SQLUpdate;
import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.InvalidObjectException;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Gang extends SQLObject {
	private String gangText = "";
	private String ownerUUID = "";
	private ArrayList<String> leaderUUIDs = new ArrayList<String>();
	private ArrayList<String> memberUUIDs = new ArrayList<String>();

	public static String TABLE_NAME = "HATS";

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" +
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`name` VARCHAR(64) NOT NULL," +
					"`gangText` VARCHAR(64) NOT NULL," +
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

	public Gang(ResultSet rs) throws InvalidObjectException, SCException, SQLException, InvalidNameException {
		this.load(rs);
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException,
			InvalidObjectException, SCException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.setGangText(rs.getString("gangText"));
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
		hashmap.put("gangText", this.getGangText());
		hashmap.put("ownerUUID", this.getOwnerUUID());
		hashmap.put("leaderUUIDs", this.saveUUIDs(this.leaderUUIDs));
		hashmap.put("memberUUIDs", this.saveUUIDs(this.memberUUIDs));
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		// TODO Remove the gang from all users
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	public void rename(String name, String gangText) throws SCException, InvalidNameException {
		SCSettings.gangs.remove(this.getName());
		String oldName = this.getName();

		this.setName(name);
		this.setGangText(gangText);

		SCSettings.gangs.put(name, this);
		try {
			this.saveNow();
		} catch (SQLException e) {
			SCLog.exception("Failed to rename gang: '"+oldName+"' -> '"+name+"'" , e);
			e.printStackTrace();
		}
	}

	public Gang(String name, String gangText, String ownerUUID) throws InvalidNameException {
		this.setName(name);
		this.setGangText(gangText);
		this.setOwnerUUID(ownerUUID);
	}
	
	public String getGangText() {
		return this.gangText;
	}
	
	public void setGangText(String string) {
		this.gangText = string;
	}

	public String getOwnerUUID() {
		return ownerUUID;
	}
	
	public Boolean canUseGang(String uuid) {
		return (this.getOwnerUUID().equals(uuid) || this.isLeader(uuid) || this.isMember(uuid));
	}
	
	public Boolean canGiveGang(String uuid) {
		return (this.getOwnerUUID().equals(uuid) || this.isLeader(uuid));
	}
	
	public Boolean canShareGang(String uuid) {
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
