package com.example.pg.dto;

import java.util.List;

public record PersonResponse(
		Long id,
		String name,
		String phone,
		String email,
		String notes,
		CurrentRoomResponse currentRoom,
		List<PastStayResponse> pastStays,
		List<RentHistoryResponse> rentHistory,
		IdentityDocumentResponse identity
) {
}
