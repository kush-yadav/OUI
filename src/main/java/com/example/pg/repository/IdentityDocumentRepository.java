package com.example.pg.repository;

import com.example.pg.dto.IdentityDocumentResponse;
import com.example.pg.entity.IdentityDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IdentityDocumentRepository extends JpaRepository<IdentityDocument, Long> {

	Optional<IdentityDocument> findByPersonId(Long personId);

	@Query("""
			select new com.example.pg.dto.IdentityDocumentResponse(d.fileName, d.contentType, d.sizeBytes)
			from IdentityDocument d
			where d.person.id = :personId
			""")
	Optional<IdentityDocumentResponse> findInfoByPersonId(@Param("personId") Long personId);

	@Query("select d.person.id from IdentityDocument d")
	List<Long> findPersonIds();

}
