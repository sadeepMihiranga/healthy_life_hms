package lk.healthylife.hms.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    /** return login user's user code */
    @Override
    public Optional<String> getCurrentAuditor() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (RequestContextHolder.getRequestAttributes() != null) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            //return Optional.of(request.getAttribute(PARTY_CODE.getValue()).toString());
            return Optional.of("CC00000001");
        } else {
            return Optional.of("CC00000001");
        }
    }
}