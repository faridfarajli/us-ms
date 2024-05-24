package az.ingress.dto;

import az.ingress.domain.UserAuthority;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEntityDto{
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String username;
    private boolean enabled;
}
