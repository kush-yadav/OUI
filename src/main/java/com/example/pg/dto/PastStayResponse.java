package com.example.pg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PastStayResponse(
		Long roomId,
		String roomName,
		int sharingCapacity,
		LocalDate moveInDate,
		LocalDate moveOutDate,
		int spots,
		BigDecimal agreedRent,
		LocalDate plannedLeaveDate
) {
}
