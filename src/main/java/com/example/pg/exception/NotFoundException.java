package com.example.pg.exception;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String detail) {
		super(detail);
	}

}
