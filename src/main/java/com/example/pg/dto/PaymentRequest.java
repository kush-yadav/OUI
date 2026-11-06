package com.example.pg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
		@NotBlank
		@Schema(example = "2026-11", description = "Calendar month, YYYY-MM")
		String month,

		@NotNull
		@DecimalMin(value = "0.01", message = "amount must be greater than zero")
		@Digits(integer = 8, fraction = 2)
		@Schema(example = "2000.00")
		BigDecimal amount,

		@NotNull
		@Schema(example = "2026-11-05")
		LocalDate paidOn,

		@Size(max = 200)
		@Schema(example = "UPI")
		String note
) {
}
