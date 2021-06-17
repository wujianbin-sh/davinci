package edp.davinci.task.executor;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import edp.davinci.commons.util.DateUtils;
import edp.davinci.commons.util.IOUtils;
import edp.davinci.core.dao.entity.User;
import edp.davinci.task.config.report.ReportTask;
import edp.davinci.task.config.report.ReportTaskTemplate;
import edp.davinci.task.config.report.ReportTaskView;
import edp.davinci.task.pojo.DataWithColumns;
import edp.davinci.task.pojo.MailAttachment;
import edp.davinci.task.pojo.MailContent;
import edp.davinci.task.pojo.WidgetQueryParam;
import edp.davinci.task.service.UserService;
import edp.davinci.task.service.ViewService;
import edp.davinci.task.util.MailUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

@Slf4j
@Data
@Component
public class ReportTaskJob implements Job {

    private ReportTask task;

    @Autowired
    UserService userService;

    @Autowired
    ViewService viewService;

    @Autowired
    MailUtils mailUtils;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String date = DateUtils.dateFormat(new Date(), "yyyyMMdd");
        String path = System.getenv("DAVINCI_TASK_HOME") + File.separator + "userfiles"
                + File.separator + "report" + File.separator + date;
        IOUtils.createFolder(path);

        String name = task.getName();
        ReportTaskTemplate template = task.getTemplate();
        String[] sendTo = task.getSendTo();

        if (!IOUtils.isFile(template.getFile())) {
            log.error("Template:{} is not exit", template.getFile());
            return;
        }

        TemplateExportParams exportParams = new TemplateExportParams(
                template.getFile());
        exportParams.setScanAllsheet(true);
        exportParams.setColForEach(true);
        Map<String, Object> exportDataMap = new HashMap<String, Object>();

        List<ReportTaskView> views = template.getViews();
        for (ReportTaskView taskView : views) {

            try {

                String key = taskView.getKey();
                Long viewId = taskView.getViewId();
                Long userId = taskView.getUserId();
                User user = userService.getUser(userId);
                WidgetQueryParam queryParam = taskView.getQueryParam();

                // excel template header
                String[] headers = taskView.getHeaderKeys();
                List<Map<String, Object>> excelHeaders = new ArrayList<Map<String, Object>>();
                Map<String, Object> excelHeaderMap = new HashMap<String, Object>();
                excelHeaders.add(excelHeaderMap);
                for (String header : headers) {
                    excelHeaderMap.put(header, header);
                }
                exportDataMap.put(key + "_header", excelHeaders);

                DataWithColumns dataWithColumns = viewService.getData(viewId, queryParam, user);
                // excel template data
                String[] columns = taskView.getColumnKeys();
                List<Map<String, Object>> excelDatas = new ArrayList<Map<String, Object>>();
                List<Map<String, Object>> data = dataWithColumns.getData();
                data.forEach(d -> {
                    Map<String, Object> excelDataMap = new HashMap<String, Object>();
                    excelDatas.add(excelDataMap);
                    for (String column : columns) {
                        excelDataMap.put(column, d.get(column));
                    }
                });
                exportDataMap.put(key + "_data", excelDatas);

            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        boolean isSendMail = false;
        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, exportDataMap);
        String tempExcelName = path + File.separator + name + "_temp.xlsx";
        try (FileOutputStream fos = new FileOutputStream(tempExcelName)) {
            workbook.write(fos);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        if (!IOUtils.isFile(tempExcelName)) {
            log.error("Temp excel:{} is not exist", tempExcelName);
            return;
        }

        // export again, sometime data not replace, this is a workaround way
        exportParams.setTemplateUrl(tempExcelName);
        workbook = ExcelExportUtil.exportExcel(exportParams, exportDataMap);
        String excelName = path + File.separator + name + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(excelName)) {
            workbook.write(fos);
            isSendMail = true;
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        if (!isSendMail) {
            return;
        }

        File file = new File(excelName);
        MailContent content = MailContent.builder()
                .subject(name)
                .to(sendTo)
                .content("This email comes from cron job on the Davinci Task.\n")
                .attachments(new MailAttachment[]{new MailAttachment(file.getName(), file)})
                .build();

        mailUtils.sendMail(content);

    }
}
