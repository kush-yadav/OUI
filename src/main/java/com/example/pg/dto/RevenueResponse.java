package com.example.pg.dto;

import java.math.BigDecimal;

public record RevenueResponse(
		String month,
		BigDecimal totalDue,
		BigDecimal collected,
		BigDecimal outstanding,
		int pendingCount,
		int partialCount,
		int paidCount
) {
}
