package edp.davinci.task.pojo;

import lombok.Data;

@Data
public class ProjectPermission {
    private Short sourcePermission = 0;
    private Short viewPermission = 0;
    private Short widgetPermission = 0;
    private Short vizPermission = 1;
    private Short schedulePermission = 0;
    private Boolean sharePermission = false;
    private Boolean downloadPermission = false;
}
