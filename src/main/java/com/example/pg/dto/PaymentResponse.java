package com.example.pg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
		Long id,
		Long personId,
		String month,
		BigDecimal amount,
		LocalDate paidOn,
		String note,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal balance,
		String status,
		PaymentScreenshotResponse screenshot
) {
}
