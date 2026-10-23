package com.bibliotech.bibliotech.respository;

import com.bibliotech.bibliotech.domain.Loan;
import com.bibliotech.bibliotech.domain.LoanStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class LoanSpecifications {

    private LoanSpecifications() {}

    public static Specification<Loan> hasMember(Long memberId) {
        return (root, query, cb) -> memberId == null ? null : cb.equal(root.get("member").get("id"), memberId);
    }

    public static Specification<Loan> hasStatus(LoanStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Loan> overdue(LocalDate today) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), LoanStatus.ACTIVE),
                cb.lessThan(root.get("dueDate"), today));
    }
}