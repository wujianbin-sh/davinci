package edp.davinci.task.config.report;

import edp.davinci.task.pojo.WidgetQueryParam;
import lombok.Data;

@Data
public class ReportTaskView {
    String key;
    Long viewId;
    String name;
    String description;
    Long userId;
    String[] headerKeys;
    String[] columnKeys;
    WidgetQueryParam queryParam;
}
