package az.ingress.dto;

import lombok.Data;

@Data
public class SignUpDto {

    private String name;

    private String surname;

    private String username;

    private String password;

    private String email;
}
