package com.example.pg.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomResponse(
		Long id,
		String name,
		int sharingCapacity,
		BigDecimal defaultMonthlyRent,
		Integer floor,
		int occupiedCount,
		int freeSpots,
		List<String> amenities,
		List<CurrentResidentResponse> currentResidents
) {
}
