package az.ingress.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignInResponse {

    private String type;
    private AccessTokenDto accessToken;
    private RefreshTokenDto refreshToken;

}
