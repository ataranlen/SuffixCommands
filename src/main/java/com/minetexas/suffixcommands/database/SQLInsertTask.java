package com.minetexas.suffixcommands.database;

import java.sql.SQLException;
import java.util.HashMap;

public class SQLInsertTask implements Runnable {

	HashMap<String, Object> hashmap;
	String tablename;
	
	public SQLInsertTask(HashMap<String, Object> hashmap, String tablename) {
		
	}

	@Override
	public void run() {
		try {
			SQL.insertNow(hashmap, tablename);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
}
