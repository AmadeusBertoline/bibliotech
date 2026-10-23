package com.bibliotech.bibliotech.dto;

import com.bibliotech.bibliotech.domain.Member;
import com.bibliotech.bibliotech.domain.MemberStatus;

public record MemberResponse(Long id, String name, String email, String phone, MemberStatus status) {

    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getStatus());
    }
}