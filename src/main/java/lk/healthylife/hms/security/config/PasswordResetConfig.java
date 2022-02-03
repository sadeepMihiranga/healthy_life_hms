package lk.healthylife.hms.security.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration wrapper class for Password reset configs
 */
@Configuration
@Getter
public class PasswordResetConfig {

    @Value("${email.callback.host}")
    private String callbackHost;

    @Value("${email.callback.port}")
    private int port;

    @Value("${reset.mobile.number}")
    private  String mobileNumber;
}