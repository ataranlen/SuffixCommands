package com.minetexas.suffixcommands.database;

import com.minetexas.suffixcommands.exception.InvalidNameException;

public class NamedObject {

	/* Unique Id of named object. */
	private int id; 
	
	/* Display name of the object. */
	private String name;
		
	public void setName(String newname) throws InvalidNameException {
		validateName(newname);
		this.name = newname;
	}

	public String getName() {
		return this.name;
	}
	
	public void setId(int i) {
		this.id = i;
	}
	
	public int getId() {
		return id;
	}
	
	private void validateName(String name) throws InvalidNameException {
		if (name == null) {
			throw new InvalidNameException();
		}
				
		switch (name.toLowerCase()) {
			case "":
			case "null":
			case "none":
			case "town":
			case "group":
			case "civ":
			case "resident":
				throw new InvalidNameException(name);
		}
	}	
}
