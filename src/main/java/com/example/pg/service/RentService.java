package com.example.pg.service;

import com.example.pg.dto.IdentityFile;
import com.example.pg.dto.PaymentRequest;
import com.example.pg.dto.PaymentResponse;
import com.example.pg.dto.PaymentScreenshotResponse;
import com.example.pg.dto.RentHistoryResponse;
import com.example.pg.dto.RentLineResponse;
import com.example.pg.dto.RentSliceResponse;
import com.example.pg.dto.RevenueResponse;
import com.example.pg.entity.Payment;
import com.example.pg.entity.PaymentScreenshot;
import com.example.pg.entity.Person;
import com.example.pg.entity.RentLine;
import com.example.pg.entity.Stay;
import com.example.pg.exception.BadRequestException;
import com.example.pg.exception.NotFoundException;
import com.example.pg.repository.PaymentRepository;
import com.example.pg.repository.PaymentScreenshotRepository;
import com.example.pg.repository.PersonRepository;
import com.example.pg.repository.RentLineRepository;
import com.example.pg.repository.StayRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class RentService {

	private static final BigDecimal ZERO = new BigDecimal("0.00");

	private final PersonRepository personRepository;
	private final StayRepository stayRepository;
	private final RentLineRepository rentLineRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentScreenshotRepository paymentScreenshotRepository;

	public RentService(
			PersonRepository personRepository,
			StayRepository stayRepository,
			RentLineRepository rentLineRepository,
			PaymentRepository paymentRepository,
			PaymentScreenshotRepository paymentScreenshotRepository) {
		this.personRepository = personRepository;
		this.stayRepository = stayRepository;
		this.rentLineRepository = rentLineRepository;
		this.paymentRepository = paymentRepository;
		this.paymentScreenshotRepository = paymentScreenshotRepository;
	}

	public List<RentLineResponse> list(String month, String status) {
		YearMonth parsedMonth = month(month);
		String parsedStatus = status(status);
		ensureMonth(parsedMonth);
		return rentLineRepository.findByMonth(parsedMonth.atDay(1)).stream()
				.filter(line -> parsedStatus == null || parsedStatus.equals(statusOf(line)))
				.map(this::toLine)
				.toList();
	}

	public List<RevenueResponse> revenue(String month) {
		if (month == null) {
			Map<LocalDate, List<RentLine>> byMonth = rentLineRepository.findAllWithDetails().stream()
					.collect(Collectors.groupingBy(RentLine::getMonth, TreeMap::new, Collectors.toList()));
			return byMonth.entrySet().stream()
					.map(entry -> summarize(YearMonth.from(entry.getKey()), entry.getValue()))
					.toList();
		}
		YearMonth parsedMonth = month(month);
		ensureMonth(parsedMonth);
		return List.of(summarize(parsedMonth, rentLineRepository.findByMonth(parsedMonth.atDay(1))));
	}

	public PaymentResponse pay(Long personId, PaymentRequest request) {
		Person person = personRepository.findById(personId)
				.orElseThrow(() -> new NotFoundException("Person not found"));
		if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BadRequestException("amount must be greater than zero");
		}
		YearMonth parsedMonth = month(request.month());
		if (parsedMonth.isAfter(currentMonth())) {
			throw new BadRequestException("Cannot record rent for a future month");
		}
		List<Stay> stays = stayRepository.findByPersonId(personId);
		if (determining(stays, parsedMonth) == null) {
			throw new BadRequestException("Person did not stay in that month");
		}
		RentLine line = ensureLine(stays, parsedMonth);
		if (line == null) {
			throw new BadRequestException("Person did not stay in that month");
		}
		BigDecimal amount = money(request.amount());
		BigDecimal balance = balance(line);
		if (amount.compareTo(balance) > 0) {
			throw new BadRequestException("Payment exceeds the balance");
		}
		Payment payment = new Payment();
		payment.setRentLine(line);
		payment.setAmount(amount);
		payment.setPaidOn(request.paidOn());
		payment.setNote(blankToNull(request.note()));
		payment = paymentRepository.saveAndFlush(payment);
		line.setAmountPaid(money(line.getAmountPaid().add(amount)));
		rentLineRepository.saveAndFlush(line);
		return toPayment(payment, person.getId());
	}

	public StoredPaymentScreenshot storeScreenshot(Long personId, Long paymentId, String originalFileName, String rawContentType, byte[] content) {
		Payment payment = payment(personId, paymentId);
		UploadedFile.Accepted accepted = UploadedFile.accept(originalFileName, rawContentType, content, "screenshot");
		PaymentScreenshot screenshot = paymentScreenshotRepository.findByPaymentId(paymentId).orElseGet(PaymentScreenshot::new);
		boolean created = screenshot.getId() == null;
		screenshot.setPayment(payment);
		screenshot.setFileName(accepted.fileName());
		screenshot.setContentType(accepted.contentType());
		screenshot.setSizeBytes(content.length);
		screenshot.setContent(content);
		paymentScreenshotRepository.saveAndFlush(screenshot);
		return new StoredPaymentScreenshot(created, new PaymentScreenshotResponse(screenshot.getFileName(), screenshot.getContentType(), screenshot.getSizeBytes()));
	}

	public IdentityFile screenshotFile(Long personId, Long paymentId) {
		payment(personId, paymentId);
		PaymentScreenshot screenshot = paymentScreenshotRepository.findByPaymentId(paymentId)
				.orElseThrow(() -> new NotFoundException("Payment screenshot not found"));
		return new IdentityFile(screenshot.getFileName(), screenshot.getContentType(), screenshot.getContent());
	}

	private Payment payment(Long personId, Long paymentId) {
		return paymentRepository.findForPerson(paymentId, personId)
				.orElseThrow(() -> new NotFoundException("Payment not found"));
	}

	public List<RentHistoryResponse> history(Long personId) {
		List<Stay> stays = stayRepository.findByPersonId(personId);
		if (stays.isEmpty()) {
			return List.of();
		}
		YearMonth first = stays.stream()
				.map(stay -> YearMonth.from(stay.getMoveInDate()))
				.min(Comparator.naturalOrder())
				.orElseThrow();
		YearMonth lastCovered = stays.stream()
				.map(stay -> stay.getMoveOutDate() == null
						? currentMonth()
						: YearMonth.from(stay.getMoveOutDate()))
				.max(Comparator.naturalOrder())
				.orElseThrow();
		YearMonth last = lastCovered.isAfter(currentMonth()) ? currentMonth() : lastCovered;
		if (!first.isAfter(last)) {
			for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
				if (month.isAfter(currentMonth())) {
					break;
				}
				if (determining(stays, month) != null) {
					ensureLine(stays, month);
				}
			}
		}
		removeUnpaidUncovered(personId, stays);
		removeUnpaidFuture();
		Map<Long, Payment> latestPayment = paymentRepository.findByPersonId(personId).stream()
				.collect(Collectors.toMap(
						payment -> payment.getRentLine().getId(),
						payment -> payment,
						(left, right) -> {
							int byDate = left.getPaidOn().compareTo(right.getPaidOn());
							if (byDate != 0) {
								return byDate > 0 ? left : right;
							}
							return left.getId() >= right.getId() ? left : right;
						}));
		return rentLineRepository.findByPersonId(personId).stream()
				.map(line -> toHistory(line, latestPayment.get(line.getId()), stays))
				.toList();
	}

	private void ensureMonth(YearMonth month) {
		removeUnpaidFuture();
		if (month.isAfter(currentMonth())) {
			return;
		}
		LocalDate start = month.atDay(1);
		Map<Long, List<Stay>> byPerson = stayRepository.findCovering(start, month.atEndOfMonth()).stream()
				.collect(Collectors.groupingBy(stay -> stay.getPerson().getId()));
		Set<Long> covered = new HashSet<>();
		for (List<Stay> stays : byPerson.values()) {
			if (determining(stays, month) != null) {
				RentLine line = ensureLine(stays, month);
				if (line != null) {
					covered.add(line.getPerson().getId());
				}
			}
		}
		List<RentLine> stale = rentLineRepository.findByMonth(start).stream()
				.filter(line -> line.getAmountPaid().compareTo(BigDecimal.ZERO) == 0)
				.filter(line -> !covered.contains(line.getPerson().getId()))
				.toList();
		if (!stale.isEmpty()) {
			rentLineRepository.deleteAll(stale);
			rentLineRepository.flush();
		}
	}

	private void removeUnpaidUncovered(Long personId, List<Stay> stays) {
		List<RentLine> stale = rentLineRepository.findByPersonId(personId).stream()
				.filter(line -> line.getAmountPaid().compareTo(BigDecimal.ZERO) == 0)
				.filter(line -> determining(stays, YearMonth.from(line.getMonth())) == null)
				.toList();
		if (!stale.isEmpty()) {
			rentLineRepository.deleteAll(stale);
			rentLineRepository.flush();
		}
	}

	private RentLine ensureLine(List<Stay> stays, YearMonth month) {
		if (month.isAfter(currentMonth())) {
			throw new BadRequestException("Cannot create rent for a future month");
		}
		MonthBill bill = bill(stays, month);
		if (bill == null) {
			return null;
		}
		LocalDate monthDate = month.atDay(1);
		return rentLineRepository.findByPersonIdAndMonth(bill.primary().getPerson().getId(), monthDate)
				.map(line -> refreshUnpaid(line, bill))
				.orElseGet(() -> createLine(bill, monthDate));
	}

	private void removeUnpaidFuture() {
		rentLineRepository.deleteUnpaidAfter(currentMonth().atDay(1));
	}

	private static YearMonth currentMonth() {
		return YearMonth.now();
	}

	private RentLine refreshUnpaid(RentLine line, MonthBill bill) {
		if (line.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
			return line;
		}
		line.setRoom(bill.primary().getRoom());
		line.setAmountDue(bill.amountDue());
		return rentLineRepository.saveAndFlush(line);
	}

	private RentLine createLine(MonthBill bill, LocalDate monthDate) {
		Stay stay = bill.primary();
		RentLine line = new RentLine();
		line.setPerson(stay.getPerson());
		line.setRoom(stay.getRoom());
		line.setMonth(monthDate);
		line.setAmountDue(bill.amountDue());
		line.setAmountPaid(ZERO);
		try {
			return rentLineRepository.saveAndFlush(line);
		} catch (DataIntegrityViolationException exception) {
			return rentLineRepository.findByPersonIdAndMonth(stay.getPerson().getId(), monthDate)
					.orElseThrow(() -> exception);
		}
	}

	private MonthBill bill(List<Stay> stays, YearMonth month) {
		List<Stay> covering = covering(stays, month).stream()
				.sorted(Comparator.comparing(Stay::getMoveInDate).thenComparing(Stay::getId))
				.toList();
		if (covering.isEmpty()) {
			return null;
		}
		int daysInMonth = month.lengthOfMonth();
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth();
		List<RentSliceResponse> occupancy = new ArrayList<>();
		BigDecimal total = ZERO;
		for (Stay stay : covering) {
			LocalDate start = stay.getMoveInDate().isAfter(monthStart) ? stay.getMoveInDate() : monthStart;
			LocalDate last = occupiedThrough(stay, stays);
			if (last == null || last.isAfter(monthEnd)) {
				last = monthEnd;
			}
			if (last.isBefore(start)) {
				continue;
			}
			int days = (int) ChronoUnit.DAYS.between(start, last) + 1;
			if (days <= 0) {
				continue;
			}
			BigDecimal slice = money(stay.getAgreedRent()
					.multiply(BigDecimal.valueOf(days))
					.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP));
			total = money(total.add(slice));
			occupancy.add(new RentSliceResponse(stay.getRoom().getName(), days, slice));
		}
		if (occupancy.isEmpty()) {
			return null;
		}
		return new MonthBill(determining(covering, month), total, occupancy);
	}

	private static LocalDate occupiedThrough(Stay stay, List<Stay> stays) {
		LocalDate moveOut = stay.getMoveOutDate();
		if (moveOut == null) {
			return null;
		}
		boolean sameDayShift = stays.stream()
				.anyMatch(other -> other != stay && other.getMoveInDate().equals(moveOut));
		return sameDayShift ? moveOut.minusDays(1) : moveOut;
	}

	private static List<Stay> covering(List<Stay> stays, YearMonth month) {
		LocalDate start = month.atDay(1);
		LocalDate end = month.atEndOfMonth();
		return stays.stream()
				.filter(stay -> !stay.getMoveInDate().isAfter(end))
				.filter(stay -> stay.getMoveOutDate() == null || !stay.getMoveOutDate().isBefore(start))
				.toList();
	}

	private static Stay determining(List<Stay> stays, YearMonth month) {
		List<Stay> covering = covering(stays, month);
		if (covering.isEmpty()) {
			return null;
		}
		LocalDate end = month.atEndOfMonth();
		List<Stay> openAtEnd = covering.stream()
				.filter(stay -> stay.getMoveOutDate() == null || stay.getMoveOutDate().isAfter(end))
				.toList();
		if (!openAtEnd.isEmpty()) {
			return openAtEnd.stream().max(Comparator.comparing(Stay::getMoveInDate)).orElseThrow();
		}
		return covering.stream().max(Comparator.comparing(Stay::getMoveOutDate)).orElseThrow();
	}

	private record MonthBill(Stay primary, BigDecimal amountDue, List<RentSliceResponse> occupancy) {
	}

	private RentLineResponse toLine(RentLine line) {
		return new RentLineResponse(
				line.getPerson().getId(),
				line.getPerson().getName(),
				line.getRoom().getId(),
				line.getRoom().getName(),
				line.getRoom().getSharingCapacity(),
				YearMonth.from(line.getMonth()).toString(),
				line.getAmountDue(),
				line.getAmountPaid(),
				balance(line),
				statusOf(line));
	}

	private RentHistoryResponse toHistory(RentLine line, Payment payment, List<Stay> stays) {
		Long paymentId = payment == null ? null : payment.getId();
		MonthBill bill = bill(stays, YearMonth.from(line.getMonth()));
		return new RentHistoryResponse(
				YearMonth.from(line.getMonth()).toString(),
				line.getAmountDue(),
				line.getAmountPaid(),
				statusOf(line),
				paymentId,
				paymentId == null ? null : paymentScreenshotRepository.findInfoByPaymentId(paymentId).orElse(null),
				bill == null ? List.of() : bill.occupancy());
	}

	private PaymentResponse toPayment(Payment payment, Long personId) {
		RentLine line = payment.getRentLine();
		return new PaymentResponse(
				payment.getId(),
				personId,
				YearMonth.from(line.getMonth()).toString(),
				payment.getAmount(),
				payment.getPaidOn(),
				payment.getNote(),
				line.getAmountDue(),
				line.getAmountPaid(),
				balance(line),
				statusOf(line),
				paymentScreenshotRepository.findInfoByPaymentId(payment.getId()).orElse(null));
	}

	private static RevenueResponse summarize(YearMonth month, List<RentLine> lines) {
		BigDecimal totalDue = ZERO;
		BigDecimal collected = ZERO;
		int pending = 0;
		int partial = 0;
		int paid = 0;
		for (RentLine line : lines) {
			totalDue = totalDue.add(line.getAmountDue());
			collected = collected.add(line.getAmountPaid());
			switch (statusOf(line)) {
				case "pending" -> pending++;
				case "partial" -> partial++;
				default -> paid++;
			}
		}
		return new RevenueResponse(
				month.toString(),
				money(totalDue),
				money(collected),
				money(totalDue.subtract(collected)),
				pending,
				partial,
				paid);
	}

	private static BigDecimal balance(RentLine line) {
		return money(line.getAmountDue().subtract(line.getAmountPaid()));
	}

	private static String statusOf(RentLine line) {
		if (line.getAmountDue().compareTo(BigDecimal.ZERO) == 0
				|| line.getAmountPaid().compareTo(line.getAmountDue()) == 0) {
			return "paid";
		}
		if (line.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
			return "pending";
		}
		return "partial";
	}

	private static YearMonth month(String value) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException("month is required");
		}
		try {
			return YearMonth.parse(value);
		} catch (DateTimeException exception) {
			throw new BadRequestException("month must be YYYY-MM");
		}
	}

	private static String status(String value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case "pending", "partial", "paid" -> value;
			default -> throw new BadRequestException("status must be pending, partial, or paid");
		};
	}

	private static BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
