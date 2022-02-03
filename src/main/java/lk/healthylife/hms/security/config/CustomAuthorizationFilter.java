package lk.healthylife.hms.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.healthylife.hms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.REQUEST_BRANCHES;
import static lk.healthylife.hms.util.constant.Constants.REQUEST_PARTY_CODE;
import static lk.healthylife.hms.util.constant.ResponseMessageKeys.*;

@Slf4j
@Component
public class CustomAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if(request.getServletPath().equals("/api/auth/token") || request.getServletPath().equals("/api/auth/token/refresh")) {
            filterChain.doFilter(request, response);
        } else {
            final String token = jwtTokenProvider.resolveToken(request);

            if(token != null) {
                try {
                    final String username = jwtTokenProvider.extractUsername(token);

                    final UserDetails userDetails = userService.loadUserByUsername(username);

                    if (!jwtTokenProvider.isTokenExpired(token)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, "", jwtTokenProvider.extractRoles(token));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        log.info("Authenticated user " + username + ", setting security context");
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        request.setAttribute(REQUEST_PARTY_CODE.getValue(), jwtTokenProvider.extractPartyCode(token));
                        request.setAttribute(REQUEST_BRANCHES.getValue(), jwtTokenProvider.extractBranches(token));
                    }
                    filterChain.doFilter(request, response);

                } catch (Exception e) {
                    log.error("Error logging in : {} ", e.getMessage());

                    e.printStackTrace();

                    Map<String ,Object> errorAttributes = new LinkedHashMap<>();

                    errorAttributes.put(KEY_DATA, null);
                    errorAttributes.put(KEY_MESSAGE, e.getMessage());
                    errorAttributes.put(KEY_SUCCESS, false);
                    errorAttributes.put(KEY_CODE, HttpServletResponse.SC_FORBIDDEN);

                    response.setHeader("Error", e.getMessage());
                    //response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setStatus(HttpServletResponse.SC_OK);

                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    new ObjectMapper().writeValue(response.getOutputStream(), errorAttributes);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }
}
