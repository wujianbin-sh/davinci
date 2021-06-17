package edp.davinci.task.util;

import edp.davinci.commons.util.StringUtils;
import edp.davinci.task.pojo.MailAttachment;
import edp.davinci.task.pojo.MailContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

@Slf4j
@Component
public class MailUtils {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.fromAddress}")
    private String fromAddress;

    @Value("${spring.mail.nickname}")
    private String nickName;

    static {
        System.setProperty("mail.mime.splitlongparameters", "false");
    }

    public void sendMail(MailContent content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        String from = StringUtils.isEmpty(fromAddress) ? username : fromAddress;
        String nickName = StringUtils.isEmpty(content.getNickName()) ? this.nickName : content.getNickName();
        try {

            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
            messageHelper.setFrom(from, nickName);
            messageHelper.setSubject(content.getSubject());
            messageHelper.setText(content.getContent());
            messageHelper.setTo(content.getTo());
            for (MailAttachment attachment : content.getAttachments()) {
                messageHelper.addAttachment(attachment.getName(), attachment.getFile());
            }

            javaMailSender.send(message);

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
}
