package com.example.pg.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomResponse(
		Long id,
		String name,
		int sharingCapacity,
		BigDecimal defaultMonthlyRent,
		Integer floor,
		Boolean ac,
		Boolean attachedBathroom,
		String notes,
		int occupiedCount,
		int freeSpots,
		List<CurrentResidentResponse> currentResidents
) {
}
