package edp.davinci.task.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;

@Data
@AllArgsConstructor
public class MailAttachment {
    private String name;
    private File file;
}
