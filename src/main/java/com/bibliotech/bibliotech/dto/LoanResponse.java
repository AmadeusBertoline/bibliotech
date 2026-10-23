package com.bibliotech.bibliotech.dto;

import com.bibliotech.bibliotech.domain.Loan;
import com.bibliotech.bibliotech.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status,
        boolean overdue,
        int renewals,
        BigDecimal fineAmount
) {
    public static LoanResponse from(Loan l, LocalDate today) {
        return new LoanResponse(
                l.getId(),
                l.getBook().getId(), l.getBook().getTitle(),
                l.getMember().getId(), l.getMember().getName(),
                l.getLoanDate(), l.getDueDate(), l.getReturnDate(),
                l.getStatus(), l.isOverdue(today), l.getRenewals(),
                l.fine(today));
    }
}