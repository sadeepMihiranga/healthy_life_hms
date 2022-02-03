package lk.healthylife.hms.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.healthylife.hms.dto.AuthenticationRequestDTO;
import lk.healthylife.hms.dto.JwtAuthenticationResponse;
import lk.healthylife.hms.dto.ResetPasswordDTO;
import lk.healthylife.hms.dto.UpdatePasswordDTO;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.security.AuthenticationGateway;
import lk.healthylife.hms.security.config.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/auth"})
public class AuthenticationController {

    private final AuthenticationGateway authenticationGateway;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationController(AuthenticationGateway authenticationGateway,
                                    AuthenticationManager authenticationManager,
                                    JwtTokenProvider jwtTokenProvider) {
        this.authenticationGateway = authenticationGateway;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** login api / create access and refresh tokens */
    @PostMapping(value = "/token", consumes = {"application/json", "application/x-www-form-urlencoded"})
    public ResponseEntity<SuccessResponse> getToken(@RequestBody String payload) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        AuthenticationRequestDTO loginRequest = mapper.readValue(payload, AuthenticationRequestDTO.class);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return SuccessResponseHandler.generateResponse(new JwtAuthenticationResponse(jwt, refreshToken));
    }

    /** token refresh api */
    @PostMapping(value = "/token/refresh")
    public ResponseEntity<JwtAuthenticationResponse> getTokenUsingRefreshToken(@RequestBody String payload) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(payload);
            String refreshToken = jsonNode.get("refreshToken").textValue();
            Map<String, String> tokenMap = jwtTokenProvider.generateTokenPairFromRefreshToken(refreshToken);
            return ResponseEntity.ok(new JwtAuthenticationResponse(tokenMap.get("jwt"), tokenMap.get("refresh")));
        } catch (JsonProcessingException e) {
            throw new OperationException("Failed to parse the JSON payload");
        }
    }

    @PostMapping(value = "/createPassword")
    public ResponseEntity<SuccessResponse> generatePasswordCreateToken(@RequestBody String resetPasswordJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ResetPasswordDTO resetPassword = mapper.readValue(resetPasswordJson, ResetPasswordDTO.class);
        SuccessResponse response = authenticationGateway
                .generateResetPasswordToken(resetPassword, "password_reset_link_email_with_username.html", "Create Account");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /** reset password email sending api for two factor authentication */
    @PostMapping(value = "/password/reset")
    public ResponseEntity<SuccessResponse> generatePasswordResetToken(@RequestBody String resetPasswordJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ResetPasswordDTO resetPassword = mapper.readValue(resetPasswordJson, ResetPasswordDTO.class);
        SuccessResponse response = authenticationGateway
                .generateResetPasswordToken(resetPassword, "password_reset_link_email.html", "Reset Password");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping(value = "/password/update")
    public ResponseEntity<SuccessResponse> updatePassword(@RequestBody String updatePasswordJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        UpdatePasswordDTO updatePassword = mapper.readValue(updatePasswordJson, UpdatePasswordDTO.class);
        SuccessResponse response = authenticationGateway.updateUserPassword(updatePassword);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /** verify two factor authentication via a pin number (mobile) */
    @GetMapping(value = "/verifyPin")
    public ResponseEntity<SuccessResponse> verifyPIN(@RequestParam("username") String username,
                                                     @RequestParam("pinNo") String pin) {
        SuccessResponse response = authenticationGateway.verifyResetPasswordForPinNo(username, pin);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /** verify two factor authentication via token (email) */
    @GetMapping(value = "/email/verify")
    public ResponseEntity<SuccessResponse> verifyEmailLink(@RequestParam("partyCode") String partyCode,
                                                           @RequestParam("token") String token) {
        SuccessResponse response = authenticationGateway.verifyResetPasswordForTokenString(partyCode, token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
