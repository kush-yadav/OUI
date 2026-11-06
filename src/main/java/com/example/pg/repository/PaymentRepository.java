package com.example.pg.repository;

import com.example.pg.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	@Query("""
			select payment from Payment payment
			join fetch payment.rentLine line
			join fetch line.person person
			where payment.id = :paymentId and person.id = :personId
			""")
	Optional<Payment> findForPerson(@Param("paymentId") Long paymentId, @Param("personId") Long personId);

}
