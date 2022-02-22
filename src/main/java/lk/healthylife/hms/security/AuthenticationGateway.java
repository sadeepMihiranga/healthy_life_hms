package lk.healthylife.hms.security;

import lk.healthylife.hms.config.EmailConfiguration;
import lk.healthylife.hms.dto.PartyContactDTO;
import lk.healthylife.hms.dto.ResetPasswordDTO;
import lk.healthylife.hms.dto.UpdatePasswordDTO;
import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.entity.TMsPartyToken;
import lk.healthylife.hms.entity.TMsUser;
import lk.healthylife.hms.exception.DataNotFoundException;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.config.repository.PartyRepository;
import lk.healthylife.hms.config.repository.UserRepository;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.security.config.PasswordResetConfig;
import lk.healthylife.hms.service.PartyContactService;
import lk.healthylife.hms.service.PartyTokenService;
import lk.healthylife.hms.service.UserService;
import lk.healthylife.hms.util.constant.Constants;
import lk.healthylife.hms.util.constant.RequestType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Strings;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static lk.healthylife.hms.util.constant.CommonReferenceCodes.*;

/**
 * Class for authentication functions
 */
@Slf4j
@Service
@Configuration
public class AuthenticationGateway {

    private final PasswordResetConfig passwordResetConfig;
    private final LocalValidatorFactoryBean localValidatorFactoryBean;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender emailSender;
    private final EmailConfiguration emailConfig;

    private final PartyTokenService partyTokenService;
    private final PartyContactService partyContactService;
    private final UserService userService;

    private final PartyRepository partyRepository;
    private final UserRepository userRepository;

    public AuthenticationGateway(PasswordResetConfig passwordResetConfig,
                                 LocalValidatorFactoryBean localValidatorFactoryBean,
                                 PartyTokenService partyTokenService,
                                 PartyContactService partyContactService,
                                 UserService userService,
                                 PartyRepository partyRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JavaMailSender emailSender,
                                 EmailConfiguration emailConfig) {
        this.passwordResetConfig = passwordResetConfig;
        this.localValidatorFactoryBean = localValidatorFactoryBean;
        this.partyTokenService = partyTokenService;
        this.partyContactService = partyContactService;
        this.userService = userService;
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.emailConfig = emailConfig;
    }

    private String formatNumber(String number) {
        String frontCharacters = number.substring(0, 3);
        String endCharacters = number.substring(number.length() - 3, number.length());
        String middleChars = number.substring(3, number.length() - 3);
        String hiddenCharacters = new String(new char[middleChars.length()]).replace("\0", "*");
        return frontCharacters + hiddenCharacters + endCharacters;
    }

    /**
     * API call to generate reset password token (via email or SMS)
     *
     * @return generate email with token string or insert to SMS log table with PIN number for SMS
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public SuccessResponse generateResetPasswordToken(ResetPasswordDTO resetPassword, String templateName, String emailSubject) {
        SuccessResponse response = null;

        if (resetPassword == null || resetPassword.getContactType() == null ||
                resetPassword.getUsername() == null || resetPassword.getContactType().isEmpty() ||
                resetPassword.getUsername().isEmpty()) {
            throw new DataNotFoundException("Username and Contact Type are Required.");
        }
        
        if (resetPassword.getContactType().equals(PARTY_CONTACT_EMAIL.getValue()))
            response = generateResetPasswordTokenEmail(resetPassword.getUsername(), templateName, emailSubject);
        else if (resetPassword.getContactType().equals(PARTY_CONTACT_MOBILE.getValue()))
            response = generateResetPasswordTokenMobile(resetPassword.getUsername());
        else
            throw new InvalidDataException("Invalid Contact Type");

        return response;
    }

    /**
     * API to verify generated token with the user code before updating reset
     * password
     *
     * @param token - token string
     * @return - party code if valid token
     * @Param partyCode - party code
     */
    public SuccessResponse verifyResetPasswordForTokenString(String partyCode, String token) {
        SuccessResponse response = null;

        if (partyCode == null || token == null)
            throw new DataNotFoundException("User Code and Token String are Required.");

        final UserDTO userDTO = userService.getUserByPartyCode(partyCode);

        TMsPartyToken tCmMsPartyToken = partyTokenService.findAllByPartyCodeToken(userDTO.getPartyCode(), token);
        if (tCmMsPartyToken == null)
            throw new InvalidDataException("Email Link is Invalid.");

        if ((tCmMsPartyToken.getToknExpiryTime().getTime() - Calendar.getInstance().getTime().getTime()) <= 0)
            throw new InvalidDataException("Email Link is Expired.");
        else
            response = new SuccessResponse(userDTO.getPartyCode(), "Valid Token", true, 1100);

        return response;
    }

    /**
     * API to verify generated PIN No with the user code before updating reset
     * password
     *
     * @return party code if valid PIN number
     * @Param partyCode - party code
     */
    public SuccessResponse verifyResetPasswordForPinNo(String username, String pinNo) {
        SuccessResponse response = null;
        if (username == null || pinNo == null)
            throw new DataNotFoundException("Username and PIN Number are Required.");

        final UserDTO userDTO = userService.getUserByUsername(username);
        if (userDTO == null)
            throw new InvalidDataException("INVALID_USERNAME");

        TMsPartyToken tCmMsPartyToken = partyTokenService.findAllByPartyCodePIN(userDTO.getPartyCode(), pinNo);
        if (tCmMsPartyToken == null)
            throw new InvalidDataException("Invalid PIN Number");

        if ((tCmMsPartyToken.getToknExpiryTime().getTime() - Calendar.getInstance().getTime().getTime()) <= 0)
            throw new InvalidDataException("Expired PIN Number");
        else
            response = new SuccessResponse(userDTO.getPartyCode(), "Valid PIN", true, 1100);

        return response;
    }

    /**
     * Generate reset password token for email contact type
     *
     * @param partyUsername
     * @return generated token string for email password recovery
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    @Transactional
    private SuccessResponse generateResetPasswordTokenEmail(String partyUsername, String templateName, String emailSubject) {
        SuccessResponse response = null;

        if (partyUsername == null || partyUsername.isEmpty())
            throw new DataNotFoundException("Username is Required.");

        final UserDTO userDTO = userService.getUserByUsername(partyUsername);
        if (userDTO == null)
            throw new InvalidDataException("Username is invalid.");

        String token = null;
        final PartyContactDTO partyContactDTO = partyContactService.getContactsByPartyCodeAndType(userDTO.getPartyCode(),
                PARTY_CONTACT_EMAIL.getValue());
        if (partyContactDTO == null) {
            response = new SuccessResponse(null, "No Email Address for the given Username", false, 1000);
            return response;
        }

        try {
            token = generateAndSaveNewPasswordToken(PARTY_CONTACT_EMAIL.getValue(), userDTO.getPartyCode());
        } catch (Exception e) {
            log.error("Auth -> generateResetPasswordTokenEmail -> AuthenticationGateway : {}", e.getMessage());
            throw new OperationException("Error when generating the token string");
        }

        try {
            emailSender.send(constructResetTokenEmail(token, userDTO.getPartyCode(), partyContactDTO.getContactNumber(),
                    templateName, emailSubject, partyUsername));
            response = new SuccessResponse(userDTO.getPartyCode(),
                    "You will receive an Email to " + partyContactDTO.getContactNumber() + " address.", true, 1100);
        } catch (Exception e) {
            log.error("Auth -> generateResetPasswordTokenEmail -> AuthenticationGateway : " + e.getMessage());
            throw new OperationException("Error when sending the email");
        }

        return response;
    }

    /**
     * Generate reset password token for mobile contact type
     *
     * @param partyUsername
     * @return generated token string for mobile password recovery
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    private SuccessResponse generateResetPasswordTokenMobile(String partyUsername) {
        SuccessResponse response = null;

        if (partyUsername == null || partyUsername.isEmpty())
            throw new DataNotFoundException("Username is Required.");

        final UserDTO userDTO = userService.getUserByUsername(partyUsername);
        if (userDTO == null)
            throw new InvalidDataException("Username is invalid");

        String token = null;
        final PartyContactDTO partyContactDTO = partyContactService.getContactsByPartyCodeAndType(userDTO.getPartyCode(),
                PARTY_CONTACT_EMAIL.getValue());

        if (partyContactDTO == null) {
            response = new SuccessResponse(null, "No Contacts for the given Username", false, 1000);
            return response;
        }

        try {
            token = generateAndSaveNewPasswordToken(PARTY_CONTACT_MOBILE.getValue(), userDTO.getPartyCode());
        } catch (Exception e) {
            log.error("Auth -> generateResetPasswordTokenMobile -> AuthenticationGateway : {}", e.getMessage());
            throw new OperationException("Error when generating the PIN Number");
        }

        // insert to sms log table
        // send sms
        try {
            response = new SuccessResponse(userDTO.getPartyCode(), "SUCCESS", true, 1100);
        } catch (Exception e) {
            log.error("Auth -> generateResetPasswordTokenMobile -> AuthenticationGateway : {}", e.getMessage());
            throw new OperationException("Error when inserting to SMS log table");
        }

        return response;
    }

    /**
     * Generate token string / pin number and save in party token table
     *
     * @param contactType (email or mobile)
     * @return Generated token
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    private String generateAndSaveNewPasswordToken(String contactType, String partyCode) {
        String token = null;

        TMsPartyToken tCmMsPartyToken = new TMsPartyToken();

        if (contactType.equals(PARTY_CONTACT_EMAIL.getValue())) {
            token = UUID.randomUUID().toString();
            tCmMsPartyToken.setToknToken(token);
        } else if (contactType.equals(PARTY_CONTACT_MOBILE.getValue())) {
            token = String.format("%06d", 100000 + new Random().nextInt(900000));
            tCmMsPartyToken.setToknPinNo(token);
        }

        tCmMsPartyToken.setToknRequestType(RequestType.PW.toString());
        tCmMsPartyToken.setToknExpiryTime();
        tCmMsPartyToken.setParty(partyRepository.findByPrtyCodeAndPrtyStatus(partyCode, Constants.STATUS_ACTIVE.getShortValue()));
        tCmMsPartyToken.setCreatedUserCode(partyCode);
        tCmMsPartyToken.setCreatedDate(new Date());
        tCmMsPartyToken.setLastModDate(new Date());
        tCmMsPartyToken.setLastModUserCode(partyCode);
        tCmMsPartyToken.setToknStatus("A");
        partyTokenService.insert(tCmMsPartyToken);

        return token;
    }

    /**
     * Generate Simple mail message
     *
     * @param to      - receiver address
     * @param subject - mail subject
     * @param text    - text message
     * @return Construct email
     */
    private MimeMessage constructEmail(String to, String subject, String text, String templateName) {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            String emailString = IOUtils.toString(new ClassPathResource("templates/" + templateName).getInputStream());
            emailString = emailString.replace("#link#", text);
            helper = new MimeMessageHelper(message, false, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailString, true);
            helper.setFrom("from_mail");
            message.setFrom(new InternetAddress("from_mail", emailConfig.getSenderName()));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Auth -> constructEmail -> AuthenticationGateway : {}", e.getMessage());
            throw new OperationException("Error while sending the email");
        }
        return message;
    }

    /**
     * Generate Email message with reset password token
     *
     * @param token          - token string
     * @param partyCode      - party code
     * @param toEmailAddress - receiver email address
     * @return send email
     */
    private MimeMessage constructResetTokenEmail(final String token, final String partyCode, final String toEmailAddress,
                                                 String templateName, String emailSubject, String username) throws UnsupportedEncodingException {
        final String url = passwordResetConfig.getCallbackHost()
                + "?id=" + partyCode
                + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
                + "&username=" + username
                + "&type=" + PARTY_CONTACT_EMAIL.getValue();
        return constructEmail(toEmailAddress, emailSubject, url, templateName);
    }

    /**
     * Update user password by given new password string (either for a given username or user code)
     *
     * @return username, if password successfully saved.
     */
    public SuccessResponse updateUserPassword(UpdatePasswordDTO updatePassword) {
        SuccessResponse response = null;
        Set<ConstraintViolation<UpdatePasswordDTO>> violations = localValidatorFactoryBean.validate(updatePassword);
        violations.stream().findFirst().ifPresent(violation -> {
            throw new DataNotFoundException(violation.getMessage());
        });

        if (updatePassword.getTokenType().equals(PARTY_CONTACT_EMAIL.getValue()))
            verifyResetPasswordForTokenString(updatePassword.getUsername(), updatePassword.getToken());
        else if (updatePassword.getTokenType().equals(PARTY_CONTACT_MOBILE.getValue()))
            verifyResetPasswordForPinNo(updatePassword.getUsername(), updatePassword.getToken());

        if (updatePassword.getTokenType().equals(PARTY_CONTACT_EMAIL.getValue())) {
            response = updUserPasswordByCode(updatePassword.getUsername(), updatePassword.getNewPassword());
            if (response.getSuccess().equals(true))
                updatePasswordTokenStatus(PARTY_CONTACT_EMAIL.getValue(), updatePassword.getUsername(), updatePassword.getToken());
        } else {
            response = updUserPasswordByUsername(updatePassword.getUsername(), updatePassword.getNewPassword());
            if (response.getSuccess().equals(true))
                updatePasswordTokenStatus(PARTY_CONTACT_MOBILE.getValue(), updatePassword.getUsername(), updatePassword.getToken());
        }

        return response;
    }

    /**
     * Update user password by given new password string for a given username
     *
     * @param partyUsername - username
     * @param newPassword   - new password string
     * @return - username if password successfully saved.
     */
    public SuccessResponse updUserPasswordByUsername(String partyUsername, String newPassword) {
        SuccessResponse response = null;

        if (partyUsername == null || newPassword == null)
            throw new DataNotFoundException("Username and New Password are Required.");

        final TMsUser tMsUser = userRepository.findByUserUsernameAndUserStatus(partyUsername, Constants.STATUS_ACTIVE.getShortValue());
        if (tMsUser == null)
            throw new InvalidDataException("Username is invalid");

        // update party table with new password
        try {
            tMsUser.setUserPassword(passwordEncoder.encode(newPassword));
            /*tMsUser.setLastModDate(new Date());
            tMsUser.setLastModUserCode(tCmMsParty.getId());*/
            userRepository.save(tMsUser);
            response = new SuccessResponse(partyUsername, "Password has been reset successfully.\n Please Signin with Newer Password", true, 1100);
        } catch (Exception e) {
            log.error("Auth -> updUserPasswordByUsername -> AuthenticationGateway : {}", e.getMessage());
            throw new OperationException("Error when updating the password");
        }

        return response;
    }

    /**
     * Update user password by given new password string and for the given user id - when password
     * is reset via email link.
     *
     * @param partyCode   - party code
     * @param newPassword - new password string
     * @return - party code if password successfully saved.
     */
    public SuccessResponse updUserPasswordByCode(String partyCode, String newPassword) {
        SuccessResponse response = null;

        if (Strings.isNullOrEmpty(partyCode) || Strings.isNullOrEmpty(newPassword))
            throw new DataNotFoundException("User Code and New Password are Required");

        final TMsUser tMsUser = userRepository.findByParty_PrtyCodeAndUserStatus(partyCode, Constants.STATUS_ACTIVE.getShortValue());
        if (tMsUser == null)
            throw new InvalidDataException("Invalid Party Code");

        try {
            tMsUser.setUserPassword(passwordEncoder.encode(newPassword));
            /*tMsUser.setLastModDate(new Date());
            tMsUser.setLastModUserCode(tCmMsParty.getId());*/
            userRepository.save(tMsUser);
            response = new SuccessResponse(tMsUser.getUserUsername(), "Password has been reset successfully.\n Please Signin with Newer Password", true, 1100);
        } catch (Exception e) {
            log.error("Auth -> updUserPasswordByCode -> AuthenticationGateway : {}", e.getMessage());
            throw new OperationException("Error when updating the password");
        }

        return response;
    }

    /**
     * Update token status once it is utilized
     *
     * @param contactType (contact code)
     * @param partyCode   (party code)
     * @Param token (email token string or mobile PIN Number]
     * @Return token sequence number
     */
    private String updatePasswordTokenStatus(String contactType, String partyCode, String token) {
        if (contactType.equals(PARTY_CONTACT_EMAIL.getValue())) {
            TMsPartyToken partyToken = partyTokenService.findAllByPartyCodeToken(partyCode, token);

            partyToken.setLastModDate(new Date());
            partyToken.setLastModUserCode(partyCode);
            partyToken.setToknStatus("I");
            partyTokenService.update(partyToken);
            return partyToken.getToknSeqNo();

        } else if (contactType.equals(PARTY_CONTACT_MOBILE.getValue())) {
            final TMsUser tMsUser = userRepository.findByParty_PrtyCodeAndUserStatus(partyCode, Constants.STATUS_ACTIVE.getShortValue());
            TMsPartyToken partyToken = partyTokenService.findAllByPartyCodePIN(tMsUser.getParty().getPrtyCode(), token);

            partyToken.setLastModDate(new Date());
            partyToken.setLastModUserCode(tMsUser.getParty().getPrtyCode());
            partyToken.setToknStatus("I");
            partyTokenService.update(partyToken);
            return partyToken.getToknSeqNo();
        }
        return null;
    }
}
