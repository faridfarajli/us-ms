package az.ingress.repository;

import az.ingress.domain.RefreshTokenEntity;
import az.ingress.domain.ResetPasswordTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResetPasswordTokenRepository extends JpaRepository<ResetPasswordTokenEntity, Long> {
    Optional<ResetPasswordTokenEntity> findByToken(String token);

    Optional<ResetPasswordTokenEntity> findByUsername(String usename);
}
