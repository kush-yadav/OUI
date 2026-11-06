package com.example.pg.service;

import com.example.pg.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

final class UploadedFile {

	private static final int MAX_BYTES = 5 * 1024 * 1024;
	private static final Set<String> TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp",
			"application/pdf");

	private UploadedFile() {
	}

	record Accepted(String fileName, String contentType) {
	}

	static Accepted accept(String originalFileName, String rawContentType, byte[] content, String fallbackName) {
		if (content == null || content.length == 0) {
			throw new BadRequestException("file must not be empty");
		}
		if (content.length > MAX_BYTES) {
			throw new BadRequestException("file must be 5 MB or smaller");
		}
		String contentType = contentType(rawContentType);
		if (!matches(contentType, content)) {
			throw new BadRequestException("file must be a JPEG, PNG, WebP, or PDF");
		}
		return new Accepted(fileName(originalFileName, fallbackName), contentType);
	}

	private static String contentType(String rawContentType) {
		if (rawContentType == null || rawContentType.isBlank()) {
			throw new BadRequestException("file must be a JPEG, PNG, WebP, or PDF");
		}
		String type = rawContentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		if ("image/jpg".equals(type)) {
			type = "image/jpeg";
		}
		if (!TYPES.contains(type)) {
			throw new BadRequestException("file must be a JPEG, PNG, WebP, or PDF");
		}
		return type;
	}

	private static boolean matches(String contentType, byte[] content) {
		return switch (contentType) {
			case "image/jpeg" -> startsWith(content, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
			case "image/png" -> startsWith(content, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
			case "application/pdf" -> startsWith(content, "%PDF".getBytes(StandardCharsets.US_ASCII));
			case "image/webp" -> content.length >= 12
					&& startsWith(content, "RIFF".getBytes(StandardCharsets.US_ASCII))
					&& content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
			default -> false;
		};
	}

	private static boolean startsWith(byte[] content, byte[] prefix) {
		if (content.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (content[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

	private static String fileName(String originalFileName, String fallbackName) {
		String name = originalFileName == null ? "" : originalFileName;
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		name = name.replace("\r", "").replace("\n", "").replace("\u0000", "").trim();
		if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
			return fallbackName;
		}
		return name.length() <= 255 ? name : name.substring(name.length() - 255);
	}

}
