package com.example.pg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MoveRequest(
		@NotNull
		@Schema(example = "1")
		Long roomId,

		@NotNull
		@Schema(example = "2026-11-03")
		LocalDate moveInDate,

		@Min(1)
		@Schema(example = "1", description = "Beds this person holds. Omit for 1.")
		Integer spots,

		@DecimalMin("0.00")
		@Digits(integer = 8, fraction = 2)
		@Schema(example = "7500.00", description = "Omit to use the room's per-person rent times spots")
		BigDecimal agreedRent
) {
}
