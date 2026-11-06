package com.example.pg.dto;

import java.math.BigDecimal;
import java.util.List;

public record RentHistoryResponse(
		String month,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		String status,
		Long paymentId,
		PaymentScreenshotResponse screenshot,
		List<RentSliceResponse> occupancy
) {
}
