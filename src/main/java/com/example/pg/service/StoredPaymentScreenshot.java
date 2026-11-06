package com.example.pg.service;

import com.example.pg.dto.PaymentScreenshotResponse;

public record StoredPaymentScreenshot(
		boolean created,
		PaymentScreenshotResponse screenshot
) {
}
