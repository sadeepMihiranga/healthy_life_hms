package lk.healthylife.hms.security.config;

import io.jsonwebtoken.lang.Assert;
import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JWTAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserService userService;

    @Autowired
    @Lazy
    private PasswordEncoder encoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationCredentialsNotFoundException {

        log.info("Authenticating with JWT provider");

        Assert.notNull(authentication, "No authentication data provided");
        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        final UserDTO userDTO = userService.getUserByUsername(username);

        if (!encoder.matches(password, userDTO.getPassword())) {
            log.error("Authentication Failed, Invalid credentials, {}", username);
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        UserDetails userDetails = userService.loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(userDetails, userDTO.getPassword(), userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
