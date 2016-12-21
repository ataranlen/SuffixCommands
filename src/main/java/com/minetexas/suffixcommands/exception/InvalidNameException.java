package com.minetexas.suffixcommands.exception;

public class InvalidNameException extends Exception {

	private static final long serialVersionUID = -697962518690144537L;
	
	public InvalidNameException() {
		super("Invalid name, name was null");
	}
	
	public InvalidNameException(String message) {
		super("Invalid name:"+message);
	}
}
