package com.example.pg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomRequest(
		@NotBlank
		@Size(max = 100)
		@Schema(example = "101")
		String name,

		@NotNull
		@Min(1)
		@Schema(example = "3", description = "How many people the room can hold")
		Integer sharingCapacity,

		@NotNull
		@DecimalMin("0.00")
		@Digits(integer = 8, fraction = 2)
		@Schema(example = "8000.00", description = "Default monthly rent per person")
		BigDecimal defaultMonthlyRent,

		@Schema(example = "1")
		Integer floor
) {
}
