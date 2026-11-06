package com.example.pg.dto;

import java.math.BigDecimal;

public record RentSliceResponse(
		String roomName,
		int days,
		BigDecimal amount
) {
}
