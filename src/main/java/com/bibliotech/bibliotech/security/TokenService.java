package com.bibliotech.bibliotech.security;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.dto.TokenResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, JwtProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public TokenResponse generate(AppUser user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(props.expiration()))
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        return new TokenResponse(token, "Bearer", props.expiration().toSeconds());
    }
}