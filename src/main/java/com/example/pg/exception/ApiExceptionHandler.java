package com.example.pg.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private static final URI BLANK_TYPE = URI.create("about:blank");

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail handleBadRequest(BadRequestException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "file must be 5 MB or smaller");
		return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
	}

	private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setType(problemType(status));
		problem.setTitle(status.getReasonPhrase());
		problem.setDetail(detail);
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		if (detail.isBlank()) {
			detail = "Invalid request content.";
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(
			@Nullable Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request) {
		if (body instanceof ProblemDetail problem) {
			if (problem.getType() == null || BLANK_TYPE.equals(problem.getType())) {
				problem.setType(problemType(statusCode));
			}
			if (problem.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
				problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
			}
		}
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.addAll(headers);
		responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
		return new ResponseEntity<>(body, responseHeaders, statusCode);
	}

	private static URI problemType(HttpStatusCode statusCode) {
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		String slug = status == null
				? String.valueOf(statusCode.value())
				: status.name().toLowerCase(Locale.ROOT).replace('_', '-');
		return URI.create("/problems/" + slug);
	}

}
