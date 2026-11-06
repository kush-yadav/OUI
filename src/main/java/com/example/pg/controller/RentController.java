package com.example.pg.controller;

import com.example.pg.dto.RentLineResponse;
import com.example.pg.dto.RevenueResponse;
import com.example.pg.service.RentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Rent")
public class RentController {

	private final RentService rentService;

	public RentController(RentService rentService) {
		this.rentService = rentService;
	}

	@GetMapping("/rent")
	@Operation(summary = "List rent for a month")
	@ApiResponse(responseCode = "200", description = "One line per person who stayed that month")
	@ApiResponse(responseCode = "400", description = "Invalid month or status", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public List<RentLineResponse> list(
			@Parameter(description = "Calendar month, YYYY-MM", example = "2026-11")
			@RequestParam(required = false) String month,
			@Parameter(description = "Omit to return pending, partial, and paid", schema = @Schema(allowableValues = {"pending", "partial", "paid"}))
			@RequestParam(required = false) String status) {
		return rentService.list(month, status);
	}

	@GetMapping("/revenue")
	@Operation(summary = "Show rent collected and still outstanding")
	@ApiResponse(responseCode = "200", description = "Every month that has a rent line, or the requested month")
	@ApiResponse(responseCode = "400", description = "Invalid month", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public List<RevenueResponse> revenue(
			@Parameter(description = "Omit to return every month that has a rent line. A requested month is returned even when nothing has been paid.", example = "2026-11")
			@RequestParam(required = false) String month) {
		return rentService.revenue(month);
	}

}
