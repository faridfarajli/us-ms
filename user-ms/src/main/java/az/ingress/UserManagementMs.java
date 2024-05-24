package az.ingress;

import az.ingress.auth.jwt.config.AuthenticationEntryPointConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(AuthenticationEntryPointConfigurer.class)
@SpringBootApplication
public class UserManagementMs {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementMs.class, args);
    }
}
