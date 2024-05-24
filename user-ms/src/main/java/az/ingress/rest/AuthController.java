package az.ingress.rest;

import az.ingress.auth.jwt.config.JwtTokenConfigProperties;
import az.ingress.dto.SignInDto;
import az.ingress.dto.SignInResponse;
import az.ingress.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

import static az.ingress.auth.jwt.config.JwtAuthRequestFilter.ACCESS_TOKEN_COOKIE;
import static az.ingress.auth.jwt.config.JwtAuthRequestFilter.REFRESH_TOKEN_COOKIE;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenConfigProperties properties;
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<SignInResponse> signIn(@Validated @RequestBody
                                                 SignInDto sign) {
        log.info("Sign in request for username {}", sign.getUsername());
        final SignInResponse signInResponse = authService.signIn(sign);
        HttpHeaders httpHeaders = new HttpHeaders();
        setCookies(httpHeaders, signInResponse);
        return new ResponseEntity<>(signInResponse, httpHeaders, HttpStatus.OK);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(@CookieValue(name = REFRESH_TOKEN_COOKIE, defaultValue = "")
                                        String refreshTokenStr) {
        authService.signOut(refreshTokenStr);
        HttpHeaders httpHeaders = new HttpHeaders();
        clearCookies(httpHeaders);
        return new ResponseEntity<>(httpHeaders, HttpStatus.NO_CONTENT);
    }

    @SneakyThrows
    @PostMapping("/refresh-token")
    public ResponseEntity<SignInResponse> refreshToken(@CookieValue(name = REFRESH_TOKEN_COOKIE, defaultValue = "")
                                                       String refreshTokenStr) {
        log.trace("Refresh token cookie is : {}", refreshTokenStr);
        SignInResponse signInResponse = authService.refreshToken(refreshTokenStr);
        HttpHeaders httpHeaders = new HttpHeaders();
        setCookies(httpHeaders, signInResponse);
        return new ResponseEntity<>(signInResponse, httpHeaders, HttpStatus.OK);
    }

    private void clearCookies(HttpHeaders httpHeaders) {
        ResponseCookie accessToken = ResponseCookie.from(ACCESS_TOKEN_COOKIE,
                        "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("LAX")
                .build();
        ResponseCookie refreshToken = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("LAX")
                .build();
        httpHeaders.add(HttpHeaders.SET_COOKIE, accessToken.toString());
        httpHeaders.add(HttpHeaders.SET_COOKIE, refreshToken.toString());
    }

    private void setCookies(HttpHeaders httpHeaders, SignInResponse signInResponse) {
        ResponseCookie accessToken = ResponseCookie.from(ACCESS_TOKEN_COOKIE,
                        signInResponse.getAccessToken().getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(properties.getJwtProperties().getTokenValidityInSeconds())
                .sameSite("LAX")
                .build();
        ResponseCookie refreshToken = ResponseCookie.from(REFRESH_TOKEN_COOKIE,
                        signInResponse.getRefreshToken().getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(properties.getJwtProperties().getRefreshTokenValidityInSeconds())
                .sameSite("LAX")
                .build();
        httpHeaders.add(HttpHeaders.SET_COOKIE, accessToken.toString());
        httpHeaders.add(HttpHeaders.SET_COOKIE, refreshToken.toString());
    }

}
