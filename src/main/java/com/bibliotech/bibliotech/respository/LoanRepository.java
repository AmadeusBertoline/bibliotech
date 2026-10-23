package com.bibliotech.bibliotech.respository;

import com.bibliotech.bibliotech.domain.Loan;
import com.bibliotech.bibliotech.domain.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);

    boolean existsByMemberIdAndStatusAndDueDateBefore(Long memberId, LoanStatus status, LocalDate date);

    boolean existsByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, LoanStatus status);

    @EntityGraph(attributePaths = {"book", "member"})
    Page<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "member"})
    Page<Loan> findByMemberId(Long memberId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"book", "member"})
    Page<Loan> findAll(org.springframework.data.jpa.domain.Specification<Loan> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "member"})
    @Query("select l from Loan l where l.id = :id")
    java.util.Optional<Loan> findDetailedById(@Param("id") Long id);
}