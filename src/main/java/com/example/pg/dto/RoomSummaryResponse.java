package com.example.pg.dto;

import java.math.BigDecimal;

public record RoomSummaryResponse(
		Long id,
		String name,
		int sharingCapacity,
		BigDecimal defaultMonthlyRent,
		int occupiedCount,
		int freeSpots
) {
}
