package lk.healthylife.hms.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static lk.healthylife.hms.util.constant.ResponseMessageKeys.*;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException, ServletException {
        logger.error("Responding with unauthorized error. Message - {}", ex.getMessage());

        Map<String ,Object> errorAttributes = new LinkedHashMap<>();

        errorAttributes.put(KEY_DATA, null);
        errorAttributes.put(KEY_MESSAGE, ex.getMessage());
        errorAttributes.put(KEY_SUCCESS, false);
        errorAttributes.put(KEY_CODE, HttpServletResponse.SC_FORBIDDEN);

        //response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), errorAttributes);
    }
}
