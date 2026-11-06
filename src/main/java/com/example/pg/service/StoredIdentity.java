package com.example.pg.service;

import com.example.pg.dto.IdentityDocumentResponse;

public record StoredIdentity(
		boolean created,
		IdentityDocumentResponse document
) {
}
