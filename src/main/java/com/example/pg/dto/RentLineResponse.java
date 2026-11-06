package com.example.pg.dto;

import java.math.BigDecimal;

public record RentLineResponse(
		Long personId,
		String personName,
		Long roomId,
		String roomName,
		int sharingCapacity,
		String month,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal balance,
		String status
) {
}
