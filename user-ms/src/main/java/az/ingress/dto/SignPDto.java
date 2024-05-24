package az.ingress.dto;

import java.util.Date;

public record SignPDto(
        Long id,
        String name,
        String surname,
        String email,
        String password,
        Date birthDate
) {


}
