package com.minetexas.suffixcommands.database;

import java.sql.SQLException;
import java.util.HashMap;

public class SQLUpdateNamedObjectTask implements Runnable {

	NamedObject obj;
	HashMap<String, Object> hashmap;
	String tablename;
	
	public SQLUpdateNamedObjectTask(NamedObject obj, HashMap<String, Object> hashmap, String tablename) {
		this.obj = obj;
		this.hashmap = hashmap;
		this.tablename = tablename;
	}
	
	@Override
	public void run() {
		try {
			if (obj.getId() == 0) {
				obj.setId(SQL.insertNow(hashmap, tablename));
			} else {
				SQL.update(obj.getId(), hashmap, tablename);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
