package lk.healthylife.hms.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.context.request.WebRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static lk.healthylife.hms.util.constant.ResponseMessageKeys.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Lazy
    @Autowired
    private JWTAuthenticationProvider jwtAuthenticationProvider;

    @Autowired
    private AuthEventPublisher authEventPublisher;

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
                .authenticationEventPublisher(authEventPublisher)
                .authenticationProvider(jwtAuthenticationProvider);
    }

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public CustomAuthorizationFilter authenticationTokenFilterBean() throws Exception {
        return new CustomAuthorizationFilter();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf()
                .disable()
                .httpBasic().disable()
                .formLogin().disable()
                .exceptionHandling()
                .authenticationEntryPoint(unauthorizedHandler)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                //.antMatchers("/api/auth/**")
                .antMatchers("/api/**")
                .permitAll()
                .anyRequest()
                .authenticated();
        http
                .addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);;
    }

    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {

                Map<String ,Object> defaultMap = super.getErrorAttributes(webRequest, options);
                Map<String ,Object> errorAttributes = new LinkedHashMap<>();

                errorAttributes.put(KEY_DATA, null);
                errorAttributes.put(KEY_MESSAGE, defaultMap.get(KEY_MESSAGE));
                errorAttributes.put(KEY_SUCCESS, false);
                //errorAttributes.put(KEY_CODE, defaultMap.get(KEY_STATUS));
                errorAttributes.put(KEY_CODE, HttpStatus.OK.value());

                return errorAttributes;
            }
        };
    }
}