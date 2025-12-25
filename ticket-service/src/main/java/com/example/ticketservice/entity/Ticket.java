package com.example.ticketservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets", indexes = {
		@Index(name = "idx_ticket_user_id", columnList = "userId"),
		@Index(name = "idx_ticket_train_id", columnList = "trainId"),
		@Index(name = "idx_ticket_status", columnList = "status"),
		@Index(name = "idx_ticket_user_status", columnList = "userId, status"),
		@Index(name = "idx_ticket_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long trainId;

	@Column(nullable = false)
	private String departureDate;

	@Column(nullable = false)
	private Integer numberOfSeats;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalPrice;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default
	private TicketStatus status = TicketStatus.PENDING;

	@Column(length = 500)
	private String cancellationReason;

	private LocalDateTime cancelledAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Version
	private Long version;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public enum TicketStatus {
		PENDING("Waiting for payment"),
		CONFIRMED("Payment confirmed"),
		CANCELLED("Ticket cancelled"),
		COMPLETED("Trip completed");

		private final String description;

		TicketStatus(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	// Business methods
	public boolean canBeCancelled() {
		return status == TicketStatus.PENDING || status == TicketStatus.CONFIRMED;
	}

	public boolean canBeConfirmed() {
		return status == TicketStatus.PENDING;
	}

	public boolean isActive() {
		return status == TicketStatus.PENDING || status == TicketStatus.CONFIRMED;
	}
}
