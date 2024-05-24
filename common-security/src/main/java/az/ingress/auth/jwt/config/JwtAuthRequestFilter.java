package az.ingress.auth.jwt.config;

import az.ingress.auth.jwt.JwtService;
import az.ingress.auth.jwt.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthRequestFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE = "jwt_accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String SWAGGER2 = "/v2/api-docs";
    private static final String SWAGGER3 = "/v3/api-docs";
    private static final String SWAGGER_UI = "/swagger-ui/**";

    private final ObjectMapper objectMapper;

    public static final String AUTHORITIES_CLAIM = "authorities";
    public static final String BEARER = "Bearer";
    public static final String AUTH_HEADER = "Authorization";
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws IOException, ServletException {
        Optional<Authentication> authOptional = authenticate(getCookie(httpServletRequest));
        authOptional.ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private Optional<Cookie> getCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = Optional.ofNullable(httpServletRequest.getCookies())
                .orElse(new Cookie[0]);
        return Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(ACCESS_TOKEN_COOKIE))
                .findFirst();
    }

    private Optional<Authentication> authenticate(Optional<Cookie> cookie) {
        if (cookie.isEmpty()) {
            return Optional.empty();
        }

        final Claims claims = jwtService.parseToken(cookie.get().getValue());
        final Collection<? extends GrantedAuthority> userAuthorities = getUserAuthorities(claims);

        UserDetails userDetails = objectMapper.convertValue(claims.get("principal"), UserDto.class);
        final UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, "", userAuthorities);
        return Optional.of(authenticationToken);
    }

    private Optional<String> getBearer(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            return Optional.empty();
        }

        final String jwt = header.substring(BEARER.length())
                .trim();
        return Optional.ofNullable(jwt);
    }

    private Collection<? extends GrantedAuthority> getUserAuthorities(Claims claims) {
        List<?> roles = claims.get(AUTHORITIES_CLAIM, List.class);
        return roles
                .stream()
                .map(a -> new SimpleGrantedAuthority(a.toString()))
                .collect(Collectors.toList());
    }


}
