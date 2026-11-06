package com.example.pg.exception;

public class BadRequestException extends RuntimeException {

	public BadRequestException(String detail) {
		super(detail);
	}

}
