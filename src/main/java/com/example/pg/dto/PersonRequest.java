package com.example.pg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonRequest(
		@NotBlank
		@Size(max = 100)
		@Schema(example = "Asha Rao")
		String name,

		@NotBlank
		@Size(max = 30)
		@Schema(example = "9876543210")
		String phone,

		@Email
		@Size(max = 200)
		@Schema(example = "asha@example.com")
		String email,

		@Size(max = 500)
		@Schema(example = "Emergency contact: Ravi, 9988776655")
		String notes
) {
}
