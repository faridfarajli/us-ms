package az.ingress.service;

import az.ingress.dto.SignInDto;
import az.ingress.dto.SignInResponse;

public interface AuthService {
    SignInResponse signIn(SignInDto sign);

    void signOut(String refreshTokenStr);

    SignInResponse refreshToken(String refreshTokenStr);
}
