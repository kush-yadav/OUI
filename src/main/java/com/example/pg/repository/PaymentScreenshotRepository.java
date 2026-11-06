package com.example.pg.repository;

import com.example.pg.dto.PaymentScreenshotResponse;
import com.example.pg.entity.PaymentScreenshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentScreenshotRepository extends JpaRepository<PaymentScreenshot, Long> {

	Optional<PaymentScreenshot> findByPaymentId(Long paymentId);

	@Query("""
			select new com.example.pg.dto.PaymentScreenshotResponse(s.fileName, s.contentType, s.sizeBytes)
			from PaymentScreenshot s
			where s.payment.id = :paymentId
			""")
	Optional<PaymentScreenshotResponse> findInfoByPaymentId(@Param("paymentId") Long paymentId);

}
