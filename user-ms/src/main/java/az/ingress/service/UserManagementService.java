package az.ingress.service;

import az.ingress.domain.UserEntity;
import az.ingress.dto.*;

public interface UserManagementService {
    ResetPasswordTokenDto forgetPasswordWithMail(String email);

    ResetPasswordTokenDto submitOtpWithMail(String reset_password_token, OtPDto otPDto);
    void resetPassword(String resetPasswordStr, NewPasswordDto newPasswordDto);

    UserEntityDto signUp(SignUpDto signUpDto);

    UserEntityDto verify(String email, String code);
}
