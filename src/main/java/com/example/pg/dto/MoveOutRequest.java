package com.example.pg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MoveOutRequest(
		@NotNull
		@Schema(example = "2026-12-03")
		LocalDate leaveDate
) {
}
