package az.ingress.auth.jwt.config;

import az.ingress.auth.jwt.config.JwtAuthRequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilterConfigurerAdapter extends
        SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final JwtAuthRequestFilter jwtAuthRequestFilter;

    @Override
    public void configure(HttpSecurity http) {
        log.info("Added auth request filter");
        http.addFilterBefore(jwtAuthRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
