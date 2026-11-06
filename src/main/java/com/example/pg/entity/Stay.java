package com.example.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stays")
@Getter
@Setter
@NoArgsConstructor
public class Stay {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	private Person person;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(name = "move_in_date", nullable = false)
	private LocalDate moveInDate;

	@Column(name = "move_out_date")
	private LocalDate moveOutDate;

	@Column(name = "agreed_rent", nullable = false, precision = 10, scale = 2)
	private BigDecimal agreedRent;

	@Column(nullable = false)
	private int spots;

	@Column(name = "planned_leave_date")
	private LocalDate plannedLeaveDate;

}
