package com.example.pg.exception;

public class ConflictException extends RuntimeException {

	public ConflictException(String detail) {
		super(detail);
	}

}
