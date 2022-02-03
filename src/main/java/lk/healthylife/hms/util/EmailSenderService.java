package lk.healthylife.hms.util;

import com.sun.istack.ByteArrayDataSource;
import lk.healthylife.hms.config.EmailConfiguration;
import lk.healthylife.hms.dto.EmailAttachmentDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Slf4j
@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailConfiguration emailConfig;

    /** can use to send any kind of main with any type of attachments */
    public void sendEmail(String[] toAddress, String fromEmailAddress, List<EmailAttachmentDTO> emailAttachmentDTOList,
                          String[] ccAddress, String subject, String msgBody, String senderName) {

        String fromEmail = emailConfig.getUsername();

        if(toAddress.length <= 0)
            throw new NoRequiredInfoException("At least One To Address is required");

        if(fromEmailAddress != null && !fromEmailAddress.isEmpty())
            fromEmail = fromEmailAddress;

        try{
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(fromEmail, senderName);
            mimeMessageHelper.setTo(InternetAddress.parse(String.join(",", toAddress)));

            /* add cc address if received */
            if(ccAddress != null && ccAddress.length > 0)
                mimeMessageHelper.setCc(InternetAddress.parse(String.join(",", ccAddress)));

            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(msgBody, true);
            mimeMessageHelper.setReplyTo(fromEmail);

            /* attached files */
            for (var attachment : emailAttachmentDTOList) {
                mimeMessageHelper.addAttachment(
                        attachment.getFileName(),
                        new ByteArrayDataSource(attachment.getFile(), attachment.getContentType().toString()));
            }

            javaMailSender.send(mimeMessage);

        }catch(MessagingException ex){
            log.error("EmailSenderService > sendEmail : Failed to generate quotation email", ex);
            throw new OperationException("Failed to generate quotation email");
        }catch (UnsupportedEncodingException ex){
            log.error("EmailSenderService > sendEmail : Failed to encode data", ex);
            throw new OperationException("Failed to encode data");
        }catch (Exception ex){
            log.error("EmailSenderService > sendEmail : Failed to generate quotation email", ex);
            throw new OperationException("Failed to generate quotation email");
        }
    }
}
