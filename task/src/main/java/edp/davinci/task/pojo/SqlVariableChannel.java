package edp.davinci.task.pojo;

import lombok.Data;

@Data
public class SqlVariableChannel {
    private String name;
    private Long tenantId;
    private Long bizId;
}
