package com.example.pg.service;

import com.example.pg.dto.CurrentResidentResponse;
import com.example.pg.dto.RoomRequest;
import com.example.pg.dto.RoomResponse;
import com.example.pg.dto.RoomSummaryResponse;
import com.example.pg.entity.Room;
import com.example.pg.entity.Stay;
import com.example.pg.exception.BadRequestException;
import com.example.pg.exception.ConflictException;
import com.example.pg.exception.NotFoundException;
import com.example.pg.repository.RoomRepository;
import com.example.pg.repository.StayRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoomService {

	private final RoomRepository roomRepository;
	private final StayRepository stayRepository;

	public RoomService(RoomRepository roomRepository, StayRepository stayRepository) {
		this.roomRepository = roomRepository;
		this.stayRepository = stayRepository;
	}

	@Transactional
	public RoomResponse create(RoomRequest request) {
		String name = name(request);
		if (roomRepository.existsByName(name)) {
			throw duplicate(name);
		}
		Room room = new Room();
		apply(room, request, name);
		return toResponse(save(room, name));
	}

	@Transactional
	public RoomResponse update(Long id, RoomRequest request) {
		Room room = room(id);
		String name = name(request);
		if (roomRepository.existsByNameAndIdNot(name, id)) {
			throw duplicate(name);
		}
		apply(room, request, name);
		return toResponse(save(room, name));
	}

	public RoomResponse get(Long id) {
		return toResponse(room(id));
	}

	public RoomResponse getByName(String name) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("Room name is required");
		}
		return toResponse(roomRepository.findByName(trimmed)
				.orElseThrow(() -> new NotFoundException("Room not found")));
	}

	public List<RoomSummaryResponse> list(String occupancy, Integer sharing) {
		Occupancy parsedOccupancy = Occupancy.parse(occupancy);
		if (sharing != null && sharing < 1) {
			throw new BadRequestException("sharing must be at least 1");
		}
		Map<Long, List<Stay>> openByRoom = stayRepository.findOpen().stream()
				.collect(Collectors.groupingBy(stay -> stay.getRoom().getId()));
		return roomRepository.findAllByOrderByNameAsc().stream()
				.filter(room -> sharing == null || room.getSharingCapacity() == sharing)
				.filter(room -> included(room, parsedOccupancy, occupiedSpots(openByRoom.getOrDefault(room.getId(), List.of()))))
				.map(room -> toSummary(room, occupiedSpots(openByRoom.getOrDefault(room.getId(), List.of()))))
				.toList();
	}

	private Room room(Long id) {
		return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));
	}

	private Room save(Room room, String name) {
		try {
			return roomRepository.saveAndFlush(room);
		} catch (DataIntegrityViolationException exception) {
			throw duplicate(name);
		}
	}

	private static void apply(Room room, RoomRequest request, String name) {
		room.setName(name);
		room.setSharingCapacity(request.sharingCapacity());
		room.setDefaultMonthlyRent(request.defaultMonthlyRent().setScale(2, RoundingMode.HALF_UP));
		room.setFloor(request.floor());
		room.setAc(request.ac());
		room.setAttachedBathroom(request.attachedBathroom());
		room.setNotes(request.notes());
	}

	private RoomResponse toResponse(Room room) {
		List<Stay> open = stayRepository.findOpenByRoomId(room.getId());
		int occupied = occupiedSpots(open);
		return new RoomResponse(
				room.getId(),
				room.getName(),
				room.getSharingCapacity(),
				room.getDefaultMonthlyRent(),
				room.getFloor(),
				room.getAc(),
				room.getAttachedBathroom(),
				room.getNotes(),
				occupied,
				room.getSharingCapacity() - occupied,
				open.stream()
						.map(stay -> new CurrentResidentResponse(stay.getPerson().getId(), stay.getPerson().getName(), stay.getSpots()))
						.toList());
	}

	private static int occupiedSpots(List<Stay> stays) {
		return stays.stream().mapToInt(Stay::getSpots).sum();
	}

	private RoomSummaryResponse toSummary(Room room, int occupied) {
		return new RoomSummaryResponse(
				room.getId(),
				room.getName(),
				room.getSharingCapacity(),
				room.getDefaultMonthlyRent(),
				occupied,
				room.getSharingCapacity() - occupied);
	}

	private boolean included(Room room, Occupancy occupancy, int occupied) {
		return switch (occupancy) {
			case ALL -> true;
			case EMPTY -> occupied == 0;
			case SEMI_EMPTY -> occupied > 0 && occupied < room.getSharingCapacity();
		};
	}

	private static String name(RoomRequest request) {
		return request.name().trim();
	}

	private static ConflictException duplicate(String name) {
		return new ConflictException("A room named " + name + " already exists");
	}

	private enum Occupancy {
		ALL,
		EMPTY,
		SEMI_EMPTY;

		private static Occupancy parse(String value) {
			if (value == null) {
				return ALL;
			}
			return switch (value) {
				case "empty" -> EMPTY;
				case "semi-empty" -> SEMI_EMPTY;
				default -> throw new BadRequestException("occupancy must be empty or semi-empty");
			};
		}
	}

}
