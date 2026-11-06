package com.example.pg.dto;

import java.math.BigDecimal;

public record RoomSummaryResponse(
		Long id,
		String name,
		Integer floor,
		int sharingCapacity,
		BigDecimal defaultMonthlyRent,
		int occupiedCount,
		int freeSpots
) {
}
