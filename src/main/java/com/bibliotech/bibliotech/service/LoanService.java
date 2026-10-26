package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.*;
import com.bibliotech.bibliotech.dto.LoanFilterStatus;
import com.bibliotech.bibliotech.dto.LoanRequest;
import com.bibliotech.bibliotech.dto.LoanResponse;
import com.bibliotech.bibliotech.exception.BusinessRuleException;
import com.bibliotech.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.bibliotech.respository.BookRepository;
import com.bibliotech.bibliotech.respository.LoanRepository;
import com.bibliotech.bibliotech.respository.MemberRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static com.bibliotech.bibliotech.respository.LoanSpecifications.*;

@Slf4j 
@Service
@Transactional(readOnly = true)
public class LoanService {

    static final int MAX_ACTIVE_LOANS = 3;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository,
            MemberRepository memberRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @CacheEvict(cacheNames = "books", key = "#req.bookId()")
    @Transactional
    public LoanResponse create(LoanRequest req) {
        LocalDate today = today();

        // Lock no livro serializa empréstimos concorrentes do mesmo título
        Book book = bookRepository.findByIdForUpdate(req.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Livro", req.bookId()));
        Member member = memberRepository.findById(req.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Membro", req.memberId()));

        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessRuleException("Membro suspenso não pode realizar empréstimos");
        }
        if (loanRepository.existsByMemberIdAndStatusAndDueDateBefore(member.getId(), LoanStatus.ACTIVE, today)) {
            throw new BusinessRuleException("Membro possui empréstimo em atraso");
        }
        if (loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE) >= MAX_ACTIVE_LOANS) {
            throw new BusinessRuleException(
                    "Membro atingiu o limite de %d empréstimos ativos".formatted(MAX_ACTIVE_LOANS));
        }
        if (loanRepository.existsByBookIdAndMemberIdAndStatus(book.getId(), member.getId(), LoanStatus.ACTIVE)) {
            throw new BusinessRuleException("Membro já possui um empréstimo ativo deste livro");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessRuleException("Não há exemplares disponíveis deste livro");
        }

        book.decrementAvailable();
        bookRepository.save(book);

        Loan loan = loanRepository.save(Loan.open(book, member, today));
        log.info("Empréstimo {} criado: livro={}, membro={}, vencimento={}",
                loan.getId(), book.getId(), member.getId(), loan.getDueDate());
        return LoanResponse.from(loan, today);
    }

    @CacheEvict(cacheNames = "books", allEntries = true)
    @Transactional
    public LoanResponse returnLoan(Long id) {
        LocalDate today = today();
        Loan loan = getOrThrow(id);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessRuleException("Este empréstimo já foi devolvido");
        }

        Book book = bookRepository.findByIdForUpdate(loan.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Livro", loan.getBook().getId()));

        loan.close(today);
        book.incrementAvailable();

        bookRepository.save(book);
        loanRepository.save(loan);

        log.info("Empréstimo {} devolvido: multa={}", loan.getId(), loan.fine(today));

        return LoanResponse.from(loan, today); // fineAmount já reflete o atraso na devolução
    }

    @Transactional
    public LoanResponse renew(Long id) {
        LocalDate today = today();
        Loan loan = getOrThrow(id);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessRuleException("Não é possível renovar um empréstimo já devolvido");
        }
        if (loan.isOverdue(today)) {
            throw new BusinessRuleException("Não é possível renovar um empréstimo em atraso");
        }
        if (!loan.canRenew()) {
            throw new BusinessRuleException("O limite de %d renovação foi atingido".formatted(Loan.MAX_RENEWALS));
        }

        loan.renew();
        loanRepository.save(loan);

        log.info("Empréstimo {} renovado até {}", loan.getId(), loan.getDueDate());

        return LoanResponse.from(loan, today);
    }

    public LoanResponse findById(Long id) {
        return LoanResponse.from(getOrThrow(id), today());
    }

    public Page<LoanResponse> search(LoanFilterStatus status, Long memberId, Pageable pageable) {
        LocalDate today = today();
        Specification<Loan> spec = Specification.allOf(
                hasMember(memberId),
                status == null ? null
                        : status == LoanFilterStatus.OVERDUE ? overdue(today)
                                : hasStatus(LoanStatus.valueOf(status.name())));
        return loanRepository.findAll(spec, pageable).map(l -> LoanResponse.from(l, today));
    }

    public Page<LoanResponse> findOverdue(Pageable pageable) {
        LocalDate today = today();
        return loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, today, pageable)
                .map(l -> LoanResponse.from(l, today));
    }

    public Page<LoanResponse> findByMember(Long memberId, Pageable pageable) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Membro", memberId);
        }
        LocalDate today = today();
        return loanRepository.findByMemberId(memberId, pageable).map(l -> LoanResponse.from(l, today));
    }

    private Loan getOrThrow(Long id) {
        return loanRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo", id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}