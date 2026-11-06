package com.example.pg.dto;

public record IdentityDocumentResponse(
		String fileName,
		String contentType,
		int sizeBytes
) {
}
