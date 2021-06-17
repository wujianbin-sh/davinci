package edp.davinci.task.config.report;

import lombok.Data;

import java.util.List;

@Data
public class ReportTask {
    private boolean enable;
    private String name;
    private ReportTaskTemplate template;
    private String startTime;
    private String endTime;
    private String executeTime;
    private String[] sendTo;
}
