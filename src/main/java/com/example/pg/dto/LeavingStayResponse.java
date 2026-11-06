package com.example.pg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeavingStayResponse(
		Long personId,
		String personName,
		Long roomId,
		String roomName,
		int sharingCapacity,
		BigDecimal agreedRent,
		LocalDate plannedLeaveDate
) {
}
