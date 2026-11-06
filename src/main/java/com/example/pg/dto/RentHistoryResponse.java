package com.example.pg.dto;

import java.math.BigDecimal;

public record RentHistoryResponse(
		String month,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		String status
) {
}
