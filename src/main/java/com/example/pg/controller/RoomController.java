package com.example.pg.controller;

import com.example.pg.dto.RoomRequest;
import com.example.pg.dto.RoomResponse;
import com.example.pg.dto.RoomSummaryResponse;
import com.example.pg.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/rooms")
@Tag(name = "Rooms")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a room")
	@ApiResponse(responseCode = "201", description = "Room created")
	@ApiResponse(responseCode = "400", description = "Invalid room", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "A room with this name already exists", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
		RoomResponse response = roomService.create(request);
		return ResponseEntity.created(URI.create("/rooms/" + response.id())).body(response);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Edit a room")
	@ApiResponse(responseCode = "200", description = "Room updated")
	@ApiResponse(responseCode = "400", description = "Invalid room", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Room not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "A room with this name already exists", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
		return roomService.update(id, request);
	}

	@GetMapping("/name/{name}")
	@Operation(summary = "Fetch a room by name")
	@ApiResponse(responseCode = "200", description = "Room profile, including who lives there now")
	@ApiResponse(responseCode = "400", description = "Room name is missing", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Room not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public RoomResponse getByName(@PathVariable String name) {
		return roomService.getByName(name);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a room profile")
	@ApiResponse(responseCode = "200", description = "Room profile, including who lives there now")
	@ApiResponse(responseCode = "404", description = "Room not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public RoomResponse get(@PathVariable Long id) {
		return roomService.get(id);
	}

	@GetMapping
	@Operation(summary = "Fetch all rooms")
	@ApiResponse(responseCode = "200", description = "Every room, with capacity, rent, occupied count, and free spots. Omit both filters to return all rooms.")
	@ApiResponse(responseCode = "400", description = "Invalid occupancy or sharing filter", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public List<RoomSummaryResponse> list(
			@Parameter(description = "Omit to return every room", schema = @Schema(allowableValues = {"empty", "semi-empty"}))
			@RequestParam(required = false) String occupancy,
			@Parameter(description = "Sharing capacity, such as 3. Omit to return every sharing")
			@RequestParam(required = false) Integer sharing) {
		return roomService.list(occupancy, sharing);
	}

}
