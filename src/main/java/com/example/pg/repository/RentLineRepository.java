package com.example.pg.repository;

import com.example.pg.entity.RentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentLineRepository extends JpaRepository<RentLine, Long> {

	Optional<RentLine> findByPersonIdAndMonth(Long personId, LocalDate month);

	@Query("""
			select line from RentLine line
			join fetch line.person
			join fetch line.room
			where line.month = :month
			order by line.person.name
			""")
	List<RentLine> findByMonth(LocalDate month);

	@Query("""
			select line from RentLine line
			join fetch line.person
			join fetch line.room
			order by line.month, line.person.name
			""")
	List<RentLine> findAllWithDetails();

	@Query("""
			select line from RentLine line
			join fetch line.room
			where line.person.id = :personId
			order by line.month
			""")
	List<RentLine> findByPersonId(Long personId);

}
