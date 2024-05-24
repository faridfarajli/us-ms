package az.ingress.service;

import az.ingress.auth.jwt.config.JwtTokenConfigProperties;
import az.ingress.common.exception.exceptions.NotFoundException;
import az.ingress.common.exception.exceptions.UnAuthorizedException;
import az.ingress.domain.ResetPasswordTokenEntity;
import az.ingress.domain.UserEntity;
import az.ingress.dto.*;
import az.ingress.repository.ResetPasswordTokenRepository;
import az.ingress.repository.UserRepository;
import az.ingress.utils.EmailSender;
import az.ingress.utils.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService{

    private final JwtTokenConfigProperties properties;
    private final ModelMapper modelMapper;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;
    @Value("${spring.verificationUrl}")
    private String verificationUrl;
    private final EmailSender emailSender;
    private void checkResetPasswordTokenIfExists(Optional<ResetPasswordTokenEntity> optionalResetPasswordTokenEntity) {
        if (optionalResetPasswordTokenEntity.isEmpty()) {
            throw new UnAuthorizedException("Reset Password token not found");
        }
    }

    @Override
    public ResetPasswordTokenDto forgetPasswordWithMail(String email) {
        Optional<UserEntity> userEntityOptional = userRepository.findByEmail(email);
        if(userEntityOptional.isPresent()){
            UserEntity user = userEntityOptional.get();
            String generatedOtp = OtpUtil.getRandomOTP();
            Optional<ResetPasswordTokenEntity> optionalResetPasswordToken = resetPasswordTokenRepository.findByUsername(user.getUsername());
            ResetPasswordTokenEntity resetPasswordToken;

            if(optionalResetPasswordToken.isPresent()){
                resetPasswordToken = optionalResetPasswordToken.get();
                resetPasswordToken.setIat(new Date());
                resetPasswordToken.setOtp(generatedOtp);
                resetPasswordToken.setUsername(user.getUsername());
                resetPasswordToken.setToken(UUID.randomUUID().toString());
                resetPasswordToken.setEat(Date.from(Instant.now().plus(Duration
                        .ofSeconds(properties.getJwtProperties()
                                .getResetPasswordTokenValidityInSeconds()))));
                resetPasswordToken.setValid(false);
            }else{
                resetPasswordToken =
                        ResetPasswordTokenEntity
                                .builder()
                                .iat(new Date())
                                .otp(generatedOtp)
                                .username(user.getUsername())
                                .token(UUID.randomUUID().toString())
                                .valid(false)
                                .eat(Date.from(Instant.now().plus(Duration
                                        .ofSeconds(properties.getJwtProperties()
                                                .getResetPasswordTokenValidityInSeconds()))))
                                .build();
            }
            resetPasswordTokenRepository.save(resetPasswordToken);
            sendMail(email, "Your OTP", generatedOtp);
            return modelMapper.map(resetPasswordToken, ResetPasswordTokenDto.class);
        }else{
            throw new NotFoundException("Email Not Found Exception");
        }
    }

    @Override
    public ResetPasswordTokenDto submitOtpWithMail(String resetPasswordStr, OtPDto otPDto) {
        final Optional<ResetPasswordTokenEntity> optionalResetPasswordTokenEntity =
                resetPasswordTokenRepository.findByToken(resetPasswordStr);
        checkResetPasswordTokenIfExists(optionalResetPasswordTokenEntity);

        final ResetPasswordTokenEntity resetPasswordToken = optionalResetPasswordTokenEntity.get();
        checkResetPasswordTokenIfValid(resetPasswordToken);

        String username = resetPasswordToken.getUsername();
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!isAccountActive(userDetails)) {
            throw new UnAuthorizedException("Account is not active");
        }

        if(resetPasswordToken.getOtp().equals(otPDto.getOtp())){
            resetPasswordToken.setValid(true);
            resetPasswordToken.setIat(new Date());
            resetPasswordToken.setEat(Date.from(Instant.now().plus(Duration
                    .ofSeconds(properties.getJwtProperties()
                            .getResetPasswordTokenValidityInSeconds()))));
            resetPasswordTokenRepository.save(resetPasswordToken);
        }
        return modelMapper.map(resetPasswordToken, ResetPasswordTokenDto.class);
    }

    @Override
    public void resetPassword(String resetPasswordStr, NewPasswordDto newPasswordDto) {
        //is refresh token exists
        final Optional<ResetPasswordTokenEntity> optionalResetPasswordTokenEntity =
                resetPasswordTokenRepository.findByToken(resetPasswordStr);
        checkResetPasswordTokenIfExists(optionalResetPasswordTokenEntity);

        final ResetPasswordTokenEntity resetPasswordToken = optionalResetPasswordTokenEntity.get();
        checkResetPasswordTokenIfValid(resetPasswordToken);

        String username = resetPasswordToken.getUsername();
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!isAccountActive(userDetails)) {
            throw new UnAuthorizedException("Account is not active");
        }
        if(resetPasswordToken.getValid()){
            issueReset(username,newPasswordDto.getNewPassword());
        }
    }

    private void issueReset(String username, String newPassword){
        UserEntity user = userRepository.findByUsername(username).get();
        BCryptPasswordEncoder bcr = new BCryptPasswordEncoder();
        String hashedString = bcr.encode(newPassword);
        user.setPassword(hashedString);
        userRepository.save(user);
    }

    @Override
    public UserEntityDto signUp(SignUpDto signUpDto) {
        if (!checkUserExist(signUpDto.getUsername(), signUpDto.getEmail())) {
            BCryptPasswordEncoder bcr = new BCryptPasswordEncoder();
            String hashedString = bcr.encode(signUpDto.getPassword());
            UserEntity user = UserEntity.builder()
                    .username(signUpDto.getUsername())
                    .password(hashedString)
                    .email(signUpDto.getEmail())
                    .verificationCode(UUID.randomUUID().toString())
                    .enabled(false)
                    .name(signUpDto.getName())
                    .surname(signUpDto.getSurname())
                    .build();
            UserEntity saved = userRepository.save(user);
            StringBuilder sb = new StringBuilder(verificationUrl).append("?email=").append(signUpDto.getEmail())
                    .append("&code=").append(user.getVerificationCode());
            String content = String.format("<h3><a href=\"%s\" target=\"_self\">VERIFY</a></h3>", sb);
            sendMail(signUpDto.getEmail(), "Verify your account", content);
            return modelMapper.map(saved, UserEntityDto.class);
        }
        return new UserEntityDto();
    }

    @Override
    public UserEntityDto verify(String email, String code) {
        Optional<UserEntity> userEntityOptional = userRepository.findByEmail(email);
        UserEntity user = userEntityOptional.get();
        if(user.getVerificationCode().equals(code)){
            user.setEnabled(true);
        }
        return modelMapper.map(user, UserEntityDto.class);
    }

    private boolean checkUserExist(String username, String email){
        if(userRepository.findByEmail(email).isPresent()){
            log.info("Email Already exist");
            return true;
        }
        if(userRepository.findByUsername(username).isPresent()){
            log.info("Username Already exist");
            return true;
        }
        return false;
    }

    private void sendMail(String email, String subject, String content) {
        emailSender.sendEmail(email, subject, content);
    }

    private void checkResetPasswordTokenIfValid(ResetPasswordTokenEntity resetPasswordToken) {
        if (!resetPasswordToken.getValid() || resetPasswordToken.getEat()
                .toInstant().isBefore(Instant.now())) {
            throw new UnAuthorizedException("Reset Password token is invalid");
        }
    }
    private boolean isAccountActive(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();

    }
}
