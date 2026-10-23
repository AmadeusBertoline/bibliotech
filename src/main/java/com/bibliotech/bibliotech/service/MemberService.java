package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.Member;
import com.bibliotech.bibliotech.dto.MemberRequest;
import com.bibliotech.bibliotech.dto.MemberResponse;
import com.bibliotech.bibliotech.exception.ConflictException;
import com.bibliotech.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.bibliotech.respository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Page<MemberResponse> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable).map(MemberResponse::from);
    }

    public MemberResponse findById(Long id) {
        return MemberResponse.from(getOrThrow(id));
    }

    @Transactional
    public MemberResponse create(MemberRequest req) {
        if (memberRepository.existsByEmail(req.email())) {
            throw new ConflictException("Já existe um membro com o e-mail " + req.email());
        }
        Member member = new Member();
        apply(member, req);
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest req) {
        Member member = getOrThrow(id);
        if (!member.getEmail().equalsIgnoreCase(req.email()) && memberRepository.existsByEmail(req.email())) {
            throw new ConflictException("Já existe um membro com o e-mail " + req.email());
        }
        apply(member, req);
        return MemberResponse.from(memberRepository.save(member));
    }

    public Member getOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membro", id));
    }

    private void apply(Member member, MemberRequest req) {
        member.setName(req.name());
        member.setEmail(req.email());
        member.setPhone(req.phone());
        if (req.status() != null) {
            member.setStatus(req.status());
        }
    }
}