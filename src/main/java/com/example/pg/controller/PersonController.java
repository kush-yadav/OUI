package com.example.pg.controller;

import com.example.pg.dto.IdentityDocumentResponse;
import com.example.pg.dto.IdentityFile;
import com.example.pg.dto.LeavingStayResponse;
import com.example.pg.dto.MoveOutRequest;
import com.example.pg.dto.MoveRequest;
import com.example.pg.dto.NoticeRequest;
import com.example.pg.dto.PaymentRequest;
import com.example.pg.dto.PaymentResponse;
import com.example.pg.dto.PaymentScreenshotResponse;
import com.example.pg.dto.PersonRequest;
import com.example.pg.dto.PersonResponse;
import com.example.pg.dto.PersonSummaryResponse;
import com.example.pg.exception.BadRequestException;
import com.example.pg.service.PersonService;
import com.example.pg.service.RentService;
import com.example.pg.service.StoredIdentity;
import com.example.pg.service.StoredPaymentScreenshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/people")
@Tag(name = "People")
public class PersonController {

	private final PersonService personService;
	private final RentService rentService;

	public PersonController(PersonService personService, RentService rentService) {
		this.personService = personService;
		this.rentService = rentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a person")
	@ApiResponse(responseCode = "201", description = "Person created")
	@ApiResponse(responseCode = "400", description = "Invalid person", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
		PersonResponse response = personService.create(request);
		return ResponseEntity.created(URI.create("/people/" + response.id())).body(response);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Edit a person")
	@ApiResponse(responseCode = "200", description = "Person updated")
	@ApiResponse(responseCode = "400", description = "Invalid person", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public PersonResponse update(@PathVariable Long id, @Valid @RequestBody PersonRequest request) {
		return personService.update(id, request);
	}

	@GetMapping("/leaving")
	@Operation(summary = "List stays with a planned leave date in the next month")
	@ApiResponse(responseCode = "200", description = "Person, room, sharing, and rent for each planned leave")
	public List<LeavingStayResponse> leaving() {
		return personService.leaving();
	}

	@GetMapping("/name/{name}")
	@Operation(summary = "Find people by name")
	@ApiResponse(responseCode = "200", description = "Full profile for each person whose name contains the search")
	@ApiResponse(responseCode = "400", description = "Name is missing", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public List<PersonResponse> findByName(@PathVariable String name) {
		return personService.findByName(name);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a person profile")
	@ApiResponse(responseCode = "200", description = "Profile, current room, past stays, rent history, and ID file details")
	@ApiResponse(responseCode = "404", description = "Person not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public PersonResponse get(@PathVariable Long id) {
		return personService.get(id);
	}

	@GetMapping
	@Operation(summary = "List people")
	@ApiResponse(responseCode = "200", description = "People who have not left, or people who have moved out")
	@ApiResponse(responseCode = "400", description = "Invalid stay filter", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public List<PersonSummaryResponse> list(
			@Parameter(description = "Omit to return every person", schema = @Schema(allowableValues = {"current", "left"}))
			@RequestParam(required = false) String stay) {
		return personService.list(stay);
	}

	@PostMapping("/{id}/move-in")
	@Operation(summary = "Move a person into a room")
	@ApiResponse(responseCode = "200", description = "Person is now in the room")
	@ApiResponse(responseCode = "400", description = "Person already has a room, or the room does not have enough free spots", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person or room not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public PersonResponse moveIn(@PathVariable Long id, @Valid @RequestBody MoveRequest request) {
		return personService.moveIn(id, request);
	}

	@PostMapping("/{id}/shift")
	@Operation(summary = "Shift a person to another room")
	@ApiResponse(responseCode = "200", description = "Previous stay closed and a new stay opened")
	@ApiResponse(responseCode = "400", description = "No current stay, same room, or the room does not have enough free spots", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person or room not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public PersonResponse shift(@PathVariable Long id, @Valid @RequestBody MoveRequest request) {
		return personService.shift(id, request);
	}

	@PostMapping("/{id}/notice")
	@Operation(summary = "Record a planned leave date")
	@ApiResponse(responseCode = "200", description = "Planned leave date saved on the current stay")
	@ApiResponse(responseCode = "400", description = "Person has no current stay", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public PersonResponse notice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
		return personService.notice(id, request);
	}

	@PostMapping("/{id}/move-out")
	@Operation(summary = "Move a person out")
	@ApiResponse(responseCode = "200", description = "Current stay closed")
	@ApiResponse(responseCode = "400", description = "Person has no current stay", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public PersonResponse moveOut(@PathVariable Long id, @Valid @RequestBody MoveOutRequest request) {
		return personService.moveOut(id, request);
	}

	@PostMapping(value = "/{id}/identity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a person's ID file")
	@ApiResponse(responseCode = "201", description = "ID file stored")
	@ApiResponse(responseCode = "200", description = "ID file replaced")
	@ApiResponse(responseCode = "400", description = "File is empty, larger than 5 MB, or not a JPEG, PNG, WebP, or PDF", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<IdentityDocumentResponse> uploadIdentity(
			@PathVariable Long id,
			@RequestParam("file") MultipartFile file) {
		StoredIdentity stored = personService.storeIdentity(id, file.getOriginalFilename(), file.getContentType(), bytes(file));
		if (stored.created()) {
			return ResponseEntity.created(URI.create("/people/" + id + "/identity")).body(stored.document());
		}
		return ResponseEntity.ok(stored.document());
	}

	@GetMapping("/{id}/identity")
	@Operation(summary = "Download a person's ID file")
	@ApiResponse(responseCode = "200", description = "ID file", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
	@ApiResponse(responseCode = "404", description = "Person or ID file not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<byte[]> downloadIdentity(@PathVariable Long id) {
		IdentityFile identityFile = personService.identityFile(id);
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(identityFile.fileName(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(identityFile.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(identityFile.content());
	}

	@PostMapping("/{id}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Record a rent payment")
	@ApiResponse(responseCode = "201", description = "Payment recorded")
	@ApiResponse(responseCode = "400", description = "Person did not stay that month, the amount is not greater than zero, or the payment exceeds the balance", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<PaymentResponse> pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
		PaymentResponse response = rentService.pay(id, request);
		return ResponseEntity.created(URI.create("/people/" + id + "/payments/" + response.id())).body(response);
	}

	@PostMapping(value = "/{id}/payments/{paymentId}/screenshot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a payment screenshot")
	@ApiResponse(responseCode = "201", description = "Screenshot stored")
	@ApiResponse(responseCode = "200", description = "Screenshot replaced")
	@ApiResponse(responseCode = "400", description = "File is empty, larger than 5 MB, or not a JPEG, PNG, WebP, or PDF", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Person or payment not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<PaymentScreenshotResponse> uploadScreenshot(
			@PathVariable Long id,
			@PathVariable Long paymentId,
			@RequestParam("file") MultipartFile file) {
		StoredPaymentScreenshot stored = rentService.storeScreenshot(id, paymentId, file.getOriginalFilename(), file.getContentType(), bytes(file));
		if (stored.created()) {
			return ResponseEntity.created(URI.create("/people/" + id + "/payments/" + paymentId + "/screenshot")).body(stored.screenshot());
		}
		return ResponseEntity.ok(stored.screenshot());
	}

	@GetMapping("/{id}/payments/{paymentId}/screenshot")
	@Operation(summary = "Download a payment screenshot")
	@ApiResponse(responseCode = "200", description = "Payment screenshot", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
	@ApiResponse(responseCode = "404", description = "Person, payment, or screenshot not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<byte[]> downloadScreenshot(@PathVariable Long id, @PathVariable Long paymentId) {
		IdentityFile screenshot = rentService.screenshotFile(id, paymentId);
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(screenshot.fileName(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(screenshot.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(screenshot.content());
	}

	private static byte[] bytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw new BadRequestException("file could not be read");
		}
	}

}
