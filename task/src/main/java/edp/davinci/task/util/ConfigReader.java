package edp.davinci.task.util;

import edp.davinci.commons.util.IOUtils;
import edp.davinci.commons.util.JSONUtils;
import edp.davinci.task.config.report.ReportTaskConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class ConfigReader {
    public static ReportTaskConfig getReportTaskConfig() {
        String path = System.getenv("DAVINCI_TASK_HOME") + File.separator + "config" + File.separator + "reporttask.json";
        try {
            String configStr = IOUtils.readTxtFileByLine(path, "UTF-8");
            ReportTaskConfig config = JSONUtils.toObject(configStr, ReportTaskConfig.class);
            return config;
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return null;
    }
}
