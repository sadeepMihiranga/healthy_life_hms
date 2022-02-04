package lk.healthylife.hms.security.config;

import io.jsonwebtoken.*;
import lk.healthylife.hms.dto.FunctionDTO;
import lk.healthylife.hms.dto.PartyDTO;
import lk.healthylife.hms.security.User;
import lk.healthylife.hms.service.PartyService;
import lk.healthylife.hms.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static lk.healthylife.hms.security.JwtTokenUtil.*;

@Component
public class JwtTokenProvider {

    @Value("${security.jwt.expire-length:30}")
    private long tokenValidPeriod; // 30m

    @Value("${security.security-realm}")
    private String realm;

    @Value("${security.jwt.client-id}")
    private String clientId;

    @Value("${security.signing-key}")
    private String secretKey;

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final PartyService partyService;

    public JwtTokenProvider(PasswordEncoder passwordEncoder,
                            UserService userService,
                            PartyService partyService) {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.partyService = partyService;
    }

    public String generateToken(Authentication authentication) {

        User userPrincipal = (User) authentication.getPrincipal();

        Claims claims = Jwts.claims().setSubject(userPrincipal.getUsername());
        claims.put(ROLES,
                userPrincipal
                        .getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        );

        claims.put(FUNCTIONS,
                userPrincipal
                        .getPermittedFunctions()
                        .stream()
                        .map(FunctionDTO::getFunctionId)
                        .collect(Collectors.toList())
        );

        final PartyDTO partyDTO = partyService.getPartyByPartyCode(userPrincipal.getPartyCode());

        return Jwts.builder()
                .setClaims(claims)
                .claim(PARTY_CODE, partyDTO.getPartyCode())
                .claim(DISPLAY_NAME, partyDTO.getFirstName())
                .claim(USER_ID, userPrincipal.getId())
                .claim(BRANCHES, userPrincipal.getBranches())
                .setSubject(userPrincipal.getUsername())
                .setAudience(clientId)
                .setIssuer(realm)
                .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(ACCESS_TOKEN_EXPIRE_1_MONTH)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        Claims claims = Jwts.claims().setSubject(userPrincipal.getUsername());
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(realm)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(LocalDateTime.now().plusMinutes(tokenValidPeriod * 12).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public Map<String, String> generateTokenPairFromRefreshToken(String refreshToken) {

        Jws<Claims> refreshClaim = Jwts.parser().setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(refreshToken);
        String username = refreshClaim.getBody().getSubject();

        if (!refreshClaim.getBody().getExpiration().before(new Date())) {
            User userDetails = (User) userService.loadUserByUsername(username);
            Map<String, String> tokenMap = new HashMap<>();

            final List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            Claims claims = Jwts.claims().setSubject(username);
            claims.put(ROLES, roles);

            final PartyDTO partyDTO = partyService.getPartyByPartyCode(userDetails.getPartyCode());

            String jwtToken = Jwts.builder()
                    .setClaims(claims)
                    .claim(PARTY_CODE, userDetails.getPartyCode())
                    .claim(DISPLAY_NAME, partyDTO.getFirstName())
                    .claim(USER_ID, userDetails.getId())
                    .claim(BRANCHES, userDetails.getBranches())
                    .setSubject(userDetails.getUsername())
                    .setAudience(clientId)
                    .setIssuer(realm)
                    .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                    .setExpiration(ACCESS_TOKEN_EXPIRE_1_MONTH)
                    .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                    .compact();

            tokenMap.put("jwt", jwtToken);
            tokenMap.put("refresh", refreshToken);
            return tokenMap;

        } else {
            throw new BadCredentialsException("Invalid or Expired refresh token");
        }
    }

    public String extractUsername(String token) {
        return extractClaimsFromToken(token).getBody().getSubject();
    }

    public String extractPartyCode(String token) {
        return extractClaimsFromToken(token).getBody().get(PARTY_CODE).toString();
    }

    public String extractBranches(String token) {
        return extractClaimsFromToken(token).getBody().get(BRANCHES).toString();
    }

    public Collection<SimpleGrantedAuthority> extractRoles(String token) {
        final String[] roles = extractClaimsFromToken(token).getBody().get(ROLES).toString().split(",");

        Collection<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
        Arrays.stream(roles).forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority(role)));

        return grantedAuthorities;
    }

    private Jws<Claims> extractClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8)))
                .parseClaimsJws(token);
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX_BEARER)) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }

    public boolean isTokenExpired(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8)))
                    .parseClaimsJws(token);
            return (claims.getBody().getExpiration().before(new Date()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Expired or invalid JWT token");
        }
    }

    public Date getTokenExpiredAt(String token) {
        Jws<Claims> claims = Jwts.parser()
                .setSigningKey(Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8)))
                .parseClaimsJws(token);
        return claims.getBody().getExpiration();
    }
}
