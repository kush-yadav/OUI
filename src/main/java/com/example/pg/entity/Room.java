package com.example.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "sharing_capacity", nullable = false)
	private int sharingCapacity;

	@Column(name = "default_monthly_rent", nullable = false, precision = 10, scale = 2)
	private BigDecimal defaultMonthlyRent;

	private Integer floor;

	private Boolean ac;

	@Column(name = "attached_bathroom")
	private Boolean attachedBathroom;

	@Column(length = 500)
	private String notes;

}
