package az.ingress.service;

import az.ingress.domain.SignUp;
import az.ingress.dto.SignPDto;
import az.ingress.dto.SignUpDto;
import az.ingress.repository.UserManageRepository;
import az.ingress.utils.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManageService {

    private final PasswordEncoder passwordEncoder;
    private final UserManageRepository userManageRepository;
    private final EmailSenderService emailSender;

    public SignUp signUp(SignPDto signUpDto) {
        SignUp signUp = new SignUp();
        signUp.setName(signUpDto.name());
        signUp.setSurname(signUpDto.surname());
        signUp.setBirthDate(signUpDto.birthDate());
        signUp.setEmail(signUpDto.email());
        signUp.setPassword(passwordEncoder.encode(signUpDto.password()));

        String token = UUID.randomUUID().toString();
        signUp.setVerificationToken(token);
        signUp.setTokenExpiryDate(LocalDateTime.now().plusDays(1));

        userManageRepository.save(signUp);

        String link = "http://localhost:8080/users/users/verify?token=" + token;
        emailSender.send(signUpDto.email(), buildEmail(signUpDto.name(), link));

        return signUp;
    }

    private String buildEmail(String name, String link) {
        return "Hello " + name + ",\n\nPlease click the following link to verify your account:\n" + link;
    }
}