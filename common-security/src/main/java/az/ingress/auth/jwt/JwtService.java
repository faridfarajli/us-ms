package az.ingress.auth.jwt;

import az.ingress.auth.jwt.config.JwtTokenConfigProperties;
import az.ingress.auth.jwt.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtService {

    private final ModelMapper modelMapper;
    private final JwtTokenConfigProperties properties;
    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        if (StringUtils.isBlank(properties.getJwtProperties().getSecret())) {
            throw new RuntimeException("Token config not found");
        }
        keyBytes = Decoders.BASE64.decode(properties.getJwtProperties().getSecret());
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String issueToken(Authentication authentication) {
        log.trace("Issue JWT token to {}", authentication.getPrincipal());

        final JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(authentication.getName())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(Duration
                        .ofSeconds(properties.getJwtProperties().getTokenValidityInSeconds()))))
                .setHeader(Map.of("type", "JWT"))
                .signWith(key, SignatureAlgorithm.HS256)
                .addClaims(Map.of("authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                        "principal", modelMapper.map(authentication.getPrincipal(), UserDto.class)));
        return jwtBuilder.compact();
    }

}
