package edp.davinci.task.runner;

import edp.davinci.commons.Constants;
import edp.davinci.commons.util.DateUtils;
import edp.davinci.commons.util.StringUtils;
import edp.davinci.task.config.report.ReportTask;
import edp.davinci.task.config.report.ReportTaskConfig;
import edp.davinci.task.executor.ReportTaskJob;
import edp.davinci.task.util.ConfigReader;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(Integer.MAX_VALUE)
public class ReportTaskRunner implements ApplicationRunner {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    public void run(ApplicationArguments args) throws Exception {
        ReportTaskConfig taskConfig = ConfigReader.getReportTaskConfig();
        assert  taskConfig != null;

        List<ReportTask> tasks = taskConfig.getTasks();
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        for (ReportTask task : tasks) {

            boolean enable = task.isEnable();
            if (!enable) {
                continue;
            }

            String name = task.getName();
            String executeTime = task.getExecuteTime();
            String startTime = task.getStartTime();
            String endTime = task.getEndTime();

            String id = name + Constants.AT_SIGN + name.hashCode();
            String group = "REPORT";

            JobDetail job = JobBuilder.newJob(ReportTaskJob.class)
            .withIdentity(id, group)
            .build();
            job.getJobDataMap().put("task", task);

            TriggerBuilder triggerBuilder = TriggerBuilder.newTrigger()
                    .withIdentity(id, group)
                    .withSchedule(CronScheduleBuilder.cronSchedule(executeTime).withMisfireHandlingInstructionDoNothing());

            if (!StringUtils.isEmpty(startTime)) {
                triggerBuilder.startAt(DateUtils.dateFormat(startTime, "yyyy-MM-dd HH:mm:ss"));
            }

            if (!StringUtils.isEmpty(endTime)) {
                triggerBuilder.endAt(DateUtils.dateFormat(endTime, "yyyy-MM-dd HH:mm:ss"));
            }

            Trigger trigger = triggerBuilder.build();

            scheduler.scheduleJob(job, trigger);
        }

        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }
}
