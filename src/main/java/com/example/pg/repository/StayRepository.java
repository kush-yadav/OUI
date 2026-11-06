package com.example.pg.repository;

import com.example.pg.entity.Stay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StayRepository extends JpaRepository<Stay, Long> {

	@Query("""
			select stay from Stay stay
			join fetch stay.person
			join fetch stay.room
			where stay.moveOutDate is null
			""")
	List<Stay> findOpen();

	@Query("""
			select stay from Stay stay
			join fetch stay.person
			where stay.room.id = :roomId and stay.moveOutDate is null
			order by stay.person.name
			""")
	List<Stay> findOpenByRoomId(Long roomId);

	@Query("""
			select stay from Stay stay
			join fetch stay.room
			where stay.person.id = :personId and stay.moveOutDate is null
			""")
	Optional<Stay> findOpenByPersonId(Long personId);

	@Query("""
			select stay from Stay stay
			join fetch stay.room
			where stay.person.id = :personId and stay.moveOutDate is not null
			order by stay.moveInDate
			""")
	List<Stay> findClosedByPersonId(Long personId);

	@Query("select distinct stay.person.id from Stay stay where stay.moveOutDate is not null")
	Set<Long> findPersonIdsWithClosedStay();

	@Query("""
			select stay from Stay stay
			join fetch stay.person
			join fetch stay.room
			where stay.moveOutDate is null
			and stay.plannedLeaveDate >= :from
			and stay.plannedLeaveDate <= :to
			order by stay.plannedLeaveDate, stay.person.name
			""")
	List<Stay> findLeaving(LocalDate from, LocalDate to);

	@Query("""
			select stay from Stay stay
			join fetch stay.person
			join fetch stay.room
			where stay.person.id = :personId
			order by stay.moveInDate
			""")
	List<Stay> findByPersonId(Long personId);

	@Query("""
			select stay from Stay stay
			join fetch stay.person
			join fetch stay.room
			where stay.moveInDate <= :monthEnd
			and (stay.moveOutDate is null or stay.moveOutDate >= :monthStart)
			""")
	List<Stay> findCovering(LocalDate monthStart, LocalDate monthEnd);

}
