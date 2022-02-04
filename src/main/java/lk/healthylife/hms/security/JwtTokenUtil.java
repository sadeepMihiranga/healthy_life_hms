package lk.healthylife.hms.security;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenUtil {

    public static final String ROLES = "roles";
    public static final String DISPLAY_NAME = "displayName";
    public static final String PARTY_CODE = "partyCode";
    public static final String BRANCHES = "branches";
    public static final String USER_ID = "userId";
    public static final String FUNCTIONS = "functions";
    public static final String TOKEN_PREFIX_BEARER = "Bearer ";

    public static final Date ACCESS_TOKEN_EXPIRE_1_MIN = new Date(System.currentTimeMillis() + (1 * 60 * 1000));
    public static final Date REFRESH_TOKEN_EXPIRE_30_MIN = new Date(System.currentTimeMillis() + (30 * 60 * 1000));
    public static final Date ACCESS_TOKEN_EXPIRE_30_MIN = new Date(System.currentTimeMillis() + (30 * 60 * 1000));
    public static final Date ACCESS_TOKEN_EXPIRE_1_MONTH = new Date(System.currentTimeMillis() + 2592000000l);
}
