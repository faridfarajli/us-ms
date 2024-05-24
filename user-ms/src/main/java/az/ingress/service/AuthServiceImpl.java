package az.ingress.service;

import az.ingress.auth.jwt.JwtService;
import az.ingress.auth.jwt.config.JwtTokenConfigProperties;
import az.ingress.common.exception.exceptions.UnAuthorizedException;
import az.ingress.domain.RefreshTokenEntity;
import az.ingress.dto.AccessTokenDto;
import az.ingress.dto.RefreshTokenDto;
import az.ingress.dto.SignInDto;
import az.ingress.dto.SignInResponse;
import az.ingress.repository.RefreshTokenRepository;
import az.ingress.repository.ResetPasswordTokenRepository;
import az.ingress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static az.ingress.auth.jwt.config.JwtAuthRequestFilter.BEARER;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final JwtTokenConfigProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;

    @Override
    public SignInResponse signIn(SignInDto sign) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(sign.getUsername(),
                sign.getPassword());
        final Authentication authentication = authenticationManager.authenticate(authenticationToken);
        AccessTokenDto accessTokenDto = new AccessTokenDto(jwtService.issueToken(authentication));
        return new SignInResponse(BEARER, accessTokenDto, issueRefreshToken(sign.getUsername()));
    }

    @Override
    public void signOut(String refreshTokenStr) {
        final Optional<RefreshTokenEntity> optionalRefreshToken =
                refreshTokenRepository.findByToken(refreshTokenStr);
        if (optionalRefreshToken.isPresent()) {
            final RefreshTokenEntity refreshToken = optionalRefreshToken.get();
            refreshToken.setValid(false);
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Override
    public SignInResponse refreshToken(String refreshTokenStr) {
        //is refresh token exists
        final Optional<RefreshTokenEntity> optionalRefreshToken =
                refreshTokenRepository.findByToken(refreshTokenStr);
        checkIfExists(optionalRefreshToken);

        final RefreshTokenEntity refreshToken = optionalRefreshToken.get();
        checkIfValid(refreshToken);

        final RefreshTokenDto refreshTokenDto = issueRefreshToken(refreshToken.getUsername());
        invalidate(refreshToken);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(refreshToken.getUsername());

        if (!isAccountActive(userDetails)) {
            throw new UnAuthorizedException("Account is not active");
        }
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
        final String jwtToken = jwtService.issueToken(authenticationToken);
        return new SignInResponse(BEARER, new AccessTokenDto(jwtToken), refreshTokenDto);
    }

    private void checkIfValid(RefreshTokenEntity refreshToken) {
        if (!refreshToken.getValid() || refreshToken.getEat()
                .toInstant().isBefore(Instant.now())) {
            throw new UnAuthorizedException("Refersh token is invalid");
        }
    }

    private void checkIfExists(Optional<RefreshTokenEntity> refreshTokenEntity) {
        if (refreshTokenEntity.isEmpty()) {
            throw new UnAuthorizedException("Refersh token not found");
        }
    }

    private RefreshTokenDto issueRefreshToken(String username) {
        RefreshTokenEntity refreshToken =
                RefreshTokenEntity
                        .builder()
                        .iat(new Date())
                        .username(username)
                        .token(UUID.randomUUID().toString())
                        .valid(true)
                        .eat(Date.from(Instant.now().plus(Duration
                                .ofSeconds(properties.getJwtProperties()
                                        .getRefreshTokenValidityInSeconds()))))
                        .build();
        final RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.save(refreshToken);
        return modelMapper.map(refreshTokenEntity, RefreshTokenDto.class);
    }

    private void invalidate(RefreshTokenEntity refreshToken) {
        refreshToken.setValid(false);
        refreshTokenRepository.save(refreshToken);
    }


    private boolean isAccountActive(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();

    }
}
