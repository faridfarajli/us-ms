package az.ingress.repository;

import az.ingress.domain.SignUp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserManageRepository extends JpaRepository<SignUp,Long> {

    SignUp findByVerificationToken(String token);
}
