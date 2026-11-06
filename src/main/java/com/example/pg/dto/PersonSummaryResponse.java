package com.example.pg.dto;

public record PersonSummaryResponse(
		Long id,
		String name,
		String phone,
		Long roomId,
		String roomName
) {
}
