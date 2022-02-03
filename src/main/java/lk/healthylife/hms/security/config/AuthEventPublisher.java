package lk.healthylife.hms.security.config;

import lk.healthylife.hms.security.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class AuthEventPublisher implements AuthenticationEventPublisher {

    public static final String REAL_IP_HEADER = "X-Real-IP";
    private static final String AUTH_SUCCESS = "S";
    private static final String AUTH_FAILED = "F";

    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        String ipAddress = request.getRemoteAddr();
        if(request.getHeader(REAL_IP_HEADER) != null)
            ipAddress = request.getHeader(REAL_IP_HEADER);

        String userAgent = request.getHeader("User-Agent");
        String user = ((User)authentication.getPrincipal()).getUsername();

        log.info("Auth Success, {}, {}, {}", user, ipAddress, userAgent);
    }

    @Override
    public void publishAuthenticationFailure(AuthenticationException e, Authentication authentication) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        String ipAddress = request.getRemoteAddr();
        if(request.getHeader(REAL_IP_HEADER) != null)
            ipAddress = request.getHeader(REAL_IP_HEADER);

        String userAgent = request.getHeader("User-Agent");
        String user = (String) authentication.getPrincipal();

        log.info("Auth Failed, {}, {}, {}", user, ipAddress, userAgent);
    }
}
