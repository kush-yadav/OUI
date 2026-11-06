package com.example.pg.dto;

public record IdentityFile(
		String fileName,
		String contentType,
		byte[] content
) {
}
