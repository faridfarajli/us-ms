package az.ingress.rest;

import az.ingress.auth.jwt.config.JwtTokenConfigProperties;
import az.ingress.domain.SignUp;
import az.ingress.dto.*;
import az.ingress.repository.UserManageRepository;
import az.ingress.service.AuthService;
import az.ingress.service.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final JwtTokenConfigProperties properties;
    private final UserManagementService userManagementService;
    private final UserManageRepository userManageRepository;

    @GetMapping("/forget-password")
    public ResponseEntity<ResetPasswordTokenDto> forgetPassword(@RequestParam("email") String email) {
        log.info("Forget password for email {}", email);
        return new ResponseEntity<>(userManagementService.forgetPasswordWithMail(email), HttpStatus.OK);
    }

    @PutMapping("/forget-password-submission")
    public void forgetPasswordSubmission(HttpServletRequest rq, @RequestBody OtPDto otPDto) {
        String reset_password_token = rq.getHeader("RESET_PASSWORD_TOKEN");
        userManagementService.submitOtpWithMail(reset_password_token, otPDto);
    }

    @PutMapping("/reset-password")
    public void resetPassword(HttpServletRequest rq, @RequestBody NewPasswordDto newPasswordDto) {
        String reset_password_token = rq.getHeader("RESET_PASSWORD_TOKEN");
        userManagementService.resetPassword(reset_password_token, newPasswordDto);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<UserEntityDto> forgetPasswordSubmission(@RequestBody SignUpDto signUpDto) {
        return ResponseEntity.ok(userManagementService.signUp(signUpDto));
    }


//    @GetMapping("/verify")
//    public ResponseEntity<UserEntityDto> verifyAccount(@RequestParam("email") String email,@RequestParam("code") String code) {
//        return ResponseEntity.ok(userManagementService.verify(email, code));
//    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token) {
        SignUp signUp = userManageRepository.findByVerificationToken(token);

        if (signUp == null || signUp.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            return "Token is invalid or expired!";
        }

        signUp.setEnabled(true);
        signUp.setVerificationToken(null);
        signUp.setTokenExpiryDate(null);
        userManageRepository.save(signUp);

        return "Account verified successfully!";
    }

}
