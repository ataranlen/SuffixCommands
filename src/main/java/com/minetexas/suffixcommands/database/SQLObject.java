package com.minetexas.suffixcommands.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.minetexas.suffixcommands.exception.InvalidNameException;
import com.minetexas.suffixcommands.exception.InvalidObjectException;
import com.minetexas.suffixcommands.exception.SCException;

/*
 * Any object that needs to be saved will extend this object so it can be
 * saved in the database.
 */
public abstract class SQLObject extends NamedObject {
	
	private boolean isDeleted = false;

	public abstract void load(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, SCException;
		
	public abstract void save();
	
	public abstract void saveNow() throws SQLException;
	
	public abstract void delete() throws SQLException;

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
	
}
