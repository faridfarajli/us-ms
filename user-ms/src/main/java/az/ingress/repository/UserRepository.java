package az.ingress.repository;

import az.ingress.domain.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = UserEntity.Fields.authorities)
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

}
