package com.bibliotech.bibliotech.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
public class Loan {

    public static final int LOAN_DAYS = 14;
    public static final int RENEWAL_DAYS = 7;
    public static final int MAX_RENEWALS = 1;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(nullable = false)
    private Integer renewals = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Loan open(Book book, Member member, LocalDate today) {
        Loan loan = new Loan();
        loan.book = book;
        loan.member = member;
        loan.loanDate = today;
        loan.dueDate = today.plusDays(LOAN_DAYS);
        return loan;
    }

    public boolean isOverdue(LocalDate today) {
        return status == LoanStatus.ACTIVE && dueDate.isBefore(today);
    }

    public boolean canRenew() {
        return renewals < MAX_RENEWALS;
    }

    public void renew() {
        dueDate = dueDate.plusDays(RENEWAL_DAYS);
        renewals++;
    }

    public void close(LocalDate today) {
        returnDate = today;
        status = LoanStatus.RETURNED;
    }

    /** Multa calculada: R$ 0,50 por dia de atraso. Se ainda ativo, é a multa acumulada até hoje. */
    public BigDecimal fine(LocalDate today) {
        LocalDate reference = returnDate != null ? returnDate : today;
        long daysLate = ChronoUnit.DAYS.between(dueDate, reference);
        return daysLate > 0 ? FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate)) : BigDecimal.ZERO;
    }
}