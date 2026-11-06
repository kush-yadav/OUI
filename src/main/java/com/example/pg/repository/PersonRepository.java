package com.example.pg.repository;

import com.example.pg.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

	List<Person> findAllByOrderByNameAsc();

	List<Person> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

}
