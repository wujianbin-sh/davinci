package edp.davinci.task.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailContent {
    private String from;
    private String nickName;

    private String subject;
    private String content;
    private String[] to;
    private MailAttachment[] attachments;
}
