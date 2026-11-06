package com.example.pg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrentRoomResponse(
		Long roomId,
		String roomName,
		int sharingCapacity,
		LocalDate moveInDate,
		int spots,
		BigDecimal agreedRent,
		LocalDate plannedLeaveDate
) {
}
