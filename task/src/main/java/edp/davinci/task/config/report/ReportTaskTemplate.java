package edp.davinci.task.config.report;

import lombok.Data;

import java.util.List;

@Data
public class ReportTaskTemplate {
    private String file;
    private String description;
    private List<ReportTaskView> views;
}
