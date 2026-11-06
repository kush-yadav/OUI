package com.example.pg.dto;

public record PaymentScreenshotResponse(
		String fileName,
		String contentType,
		int sizeBytes
) {
}
