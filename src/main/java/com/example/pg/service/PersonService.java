package com.example.pg.service;

import com.example.pg.dto.CurrentRoomResponse;
import com.example.pg.dto.IdentityDocumentResponse;
import com.example.pg.dto.IdentityFile;
import com.example.pg.dto.LeavingStayResponse;
import com.example.pg.dto.MoveOutRequest;
import com.example.pg.dto.MoveRequest;
import com.example.pg.dto.NoticeRequest;
import com.example.pg.dto.PastStayResponse;
import com.example.pg.dto.PersonRequest;
import com.example.pg.dto.PersonResponse;
import com.example.pg.dto.PersonSummaryResponse;
import com.example.pg.entity.IdentityDocument;
import com.example.pg.entity.Person;
import com.example.pg.entity.Room;
import com.example.pg.entity.Stay;
import com.example.pg.exception.BadRequestException;
import com.example.pg.exception.NotFoundException;
import com.example.pg.repository.IdentityDocumentRepository;
import com.example.pg.repository.PersonRepository;
import com.example.pg.repository.RoomRepository;
import com.example.pg.repository.StayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PersonService {

	private final PersonRepository personRepository;
	private final RoomRepository roomRepository;
	private final StayRepository stayRepository;
	private final IdentityDocumentRepository identityDocumentRepository;
	private final RentService rentService;

	public PersonService(
			PersonRepository personRepository,
			RoomRepository roomRepository,
			StayRepository stayRepository,
			IdentityDocumentRepository identityDocumentRepository,
			RentService rentService) {
		this.personRepository = personRepository;
		this.roomRepository = roomRepository;
		this.stayRepository = stayRepository;
		this.identityDocumentRepository = identityDocumentRepository;
		this.rentService = rentService;
	}

	@Transactional
	public PersonResponse create(PersonRequest request) {
		Person person = new Person();
		apply(person, request);
		return profile(personRepository.saveAndFlush(person));
	}

	@Transactional
	public PersonResponse update(Long id, PersonRequest request) {
		Person person = person(id);
		apply(person, request);
		return profile(personRepository.saveAndFlush(person));
	}

	@Transactional
	public PersonResponse get(Long id) {
		return profile(person(id));
	}

	@Transactional
	public List<PersonResponse> findByName(String name) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("Name is required");
		}
		return personRepository.findByNameContainingIgnoreCaseOrderByNameAsc(trimmed).stream()
				.map(this::profile)
				.toList();
	}

	public List<PersonSummaryResponse> list(String stay) {
		StayFilter filter = StayFilter.parse(stay);
		Map<Long, Stay> openByPerson = stayRepository.findOpen().stream()
				.collect(Collectors.toMap(open -> open.getPerson().getId(), Function.identity()));
		Set<Long> peopleWithClosedStay = stayRepository.findPersonIdsWithClosedStay();
		return personRepository.findAllByOrderByNameAsc().stream()
				.filter(person -> filter.includes(left(person.getId(), openByPerson, peopleWithClosedStay)))
				.map(person -> toSummary(person, openByPerson.get(person.getId())))
				.toList();
	}

	public List<LeavingStayResponse> leaving() {
		LocalDate today = LocalDate.now();
		return stayRepository.findLeaving(today, today.plusMonths(1)).stream()
				.map(PersonService::toLeaving)
				.toList();
	}

	@Transactional
	public PersonResponse moveIn(Long id, MoveRequest request) {
		Person person = person(id);
		if (stayRepository.findOpenByPersonId(id).isPresent()) {
			throw new BadRequestException("Person already has a room");
		}
		Room room = room(request.roomId());
		int spots = spots(request);
		requireSpace(room, spots);
		stayRepository.saveAndFlush(openStay(person, room, request, spots));
		return profile(person);
	}

	@Transactional
	public PersonResponse shift(Long id, MoveRequest request) {
		Person person = person(id);
		Stay current = currentStay(id);
		if (request.moveInDate().isBefore(current.getMoveInDate())) {
			throw new BadRequestException("Move-in date must be on or after the current move-in date");
		}
		Room room = room(request.roomId());
		if (room.getId().equals(current.getRoom().getId())) {
			throw new BadRequestException("Person already lives in this room");
		}
		int spots = spots(request);
		requireSpace(room, spots);
		current.setMoveOutDate(request.moveInDate());
		stayRepository.saveAndFlush(current);
		stayRepository.saveAndFlush(openStay(person, room, request, spots));
		return profile(person);
	}

	@Transactional
	public PersonResponse notice(Long id, NoticeRequest request) {
		person(id);
		Stay current = currentStay(id);
		if (request.plannedLeaveDate().isBefore(current.getMoveInDate())) {
			throw new BadRequestException("Planned leave date must be on or after the move-in date");
		}
		current.setPlannedLeaveDate(request.plannedLeaveDate());
		stayRepository.saveAndFlush(current);
		return profile(current.getPerson());
	}

	@Transactional
	public PersonResponse moveOut(Long id, MoveOutRequest request) {
		person(id);
		Stay current = currentStay(id);
		if (request.leaveDate().isBefore(current.getMoveInDate())) {
			throw new BadRequestException("Leave date must be on or after the move-in date");
		}
		current.setMoveOutDate(request.leaveDate());
		stayRepository.saveAndFlush(current);
		return profile(current.getPerson());
	}

	private Stay currentStay(Long personId) {
		return stayRepository.findOpenByPersonId(personId)
				.orElseThrow(() -> new BadRequestException("Person has no current stay"));
	}

	private void requireSpace(Room room, int spots) {
		if (spots < 1) {
			throw new BadRequestException("spots must be at least 1");
		}
		int occupied = stayRepository.findOpenByRoomId(room.getId()).stream()
				.mapToInt(Stay::getSpots)
				.sum();
		if (spots > room.getSharingCapacity() - occupied) {
			throw new BadRequestException("Room does not have enough free spots");
		}
	}

	private Stay openStay(Person person, Room room, MoveRequest request, int spots) {
		Stay stay = new Stay();
		stay.setPerson(person);
		stay.setRoom(room);
		stay.setMoveInDate(request.moveInDate());
		stay.setSpots(spots);
		stay.setAgreedRent(agreedRent(request, room, spots));
		return stay;
	}

	private PersonResponse profile(Person person) {
		Stay current = stayRepository.findOpenByPersonId(person.getId()).orElse(null);
		List<PastStayResponse> pastStays = stayRepository.findClosedByPersonId(person.getId()).stream()
				.map(PersonService::toPastStay)
				.toList();
		return new PersonResponse(
				person.getId(),
				person.getName(),
				person.getPhone(),
				person.getEmail(),
				person.getNotes(),
				current == null ? null : toCurrentRoom(current),
				pastStays,
				rentService.history(person.getId()),
				identityDocumentRepository.findInfoByPersonId(person.getId()).orElse(null));
	}

	@Transactional
	public StoredIdentity storeIdentity(Long personId, String originalFileName, String rawContentType, byte[] content) {
		Person person = person(personId);
		UploadedFile.Accepted accepted = UploadedFile.accept(originalFileName, rawContentType, content, "id");
		IdentityDocument document = identityDocumentRepository.findByPersonId(personId).orElseGet(IdentityDocument::new);
		boolean created = document.getId() == null;
		document.setPerson(person);
		document.setFileName(accepted.fileName());
		document.setContentType(accepted.contentType());
		document.setSizeBytes(content.length);
		document.setContent(content);
		identityDocumentRepository.saveAndFlush(document);
		return new StoredIdentity(created, new IdentityDocumentResponse(document.getFileName(), document.getContentType(), document.getSizeBytes()));
	}

	public IdentityFile identityFile(Long personId) {
		person(personId);
		IdentityDocument document = identityDocumentRepository.findByPersonId(personId)
				.orElseThrow(() -> new NotFoundException("Identity document not found"));
		return new IdentityFile(document.getFileName(), document.getContentType(), document.getContent());
	}

	private static void apply(Person person, PersonRequest request) {
		person.setName(request.name().trim());
		person.setPhone(request.phone().trim());
		person.setEmail(blankToNull(request.email()));
		person.setNotes(blankToNull(request.notes()));
	}

	private static PersonSummaryResponse toSummary(Person person, Stay current) {
		if (current == null) {
			return new PersonSummaryResponse(person.getId(), person.getName(), person.getPhone(), null, null);
		}
		return new PersonSummaryResponse(
				person.getId(),
				person.getName(),
				person.getPhone(),
				current.getRoom().getId(),
				current.getRoom().getName());
	}

	private static CurrentRoomResponse toCurrentRoom(Stay stay) {
		return new CurrentRoomResponse(
				stay.getRoom().getId(),
				stay.getRoom().getName(),
				stay.getRoom().getSharingCapacity(),
				stay.getMoveInDate(),
				stay.getSpots(),
				stay.getAgreedRent(),
				stay.getPlannedLeaveDate());
	}

	private static PastStayResponse toPastStay(Stay stay) {
		return new PastStayResponse(
				stay.getRoom().getId(),
				stay.getRoom().getName(),
				stay.getRoom().getSharingCapacity(),
				stay.getMoveInDate(),
				stay.getMoveOutDate(),
				stay.getSpots(),
				stay.getAgreedRent(),
				stay.getPlannedLeaveDate());
	}

	private static LeavingStayResponse toLeaving(Stay stay) {
		return new LeavingStayResponse(
				stay.getPerson().getId(),
				stay.getPerson().getName(),
				stay.getRoom().getId(),
				stay.getRoom().getName(),
				stay.getRoom().getSharingCapacity(),
				stay.getAgreedRent(),
				stay.getPlannedLeaveDate());
	}

	private static int spots(MoveRequest request) {
		return request.spots() == null ? 1 : request.spots();
	}

	private static BigDecimal agreedRent(MoveRequest request, Room room, int spots) {
		if (request.agreedRent() == null) {
			return room.getDefaultMonthlyRent()
					.multiply(BigDecimal.valueOf(spots))
					.setScale(2, RoundingMode.HALF_UP);
		}
		return request.agreedRent().setScale(2, RoundingMode.HALF_UP);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static boolean left(Long personId, Map<Long, Stay> openByPerson, Set<Long> peopleWithClosedStay) {
		return !openByPerson.containsKey(personId) && peopleWithClosedStay.contains(personId);
	}

	private Person person(Long id) {
		return personRepository.findById(id).orElseThrow(() -> new NotFoundException("Person not found"));
	}

	private Room room(Long id) {
		return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));
	}

	private enum StayFilter {
		ALL,
		CURRENT,
		LEFT;

		private boolean includes(boolean hasLeft) {
			return switch (this) {
				case ALL -> true;
				case CURRENT -> !hasLeft;
				case LEFT -> hasLeft;
			};
		}

		private static StayFilter parse(String value) {
			if (value == null) {
				return ALL;
			}
			return switch (value) {
				case "current" -> CURRENT;
				case "left" -> LEFT;
				default -> throw new BadRequestException("stay must be current or left");
			};
		}
	}

}
