package edp.davinci.service.excel;

import com.google.common.base.Stopwatch;
import edp.davinci.core.enums.ActionEnum;
import edp.davinci.dto.cronJobDto.MsgMailExcel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PivotTableSheetWorker<T> extends SheetWorker {
    Logger logger;

    private WorkBookContext workBookContext;

    private SheetContext sheetContext;

    private Sheet pivotSheet;

    private int maxRows = 1000000;

    private CellStyle header;

    private int nextRowNum = 0;

    public PivotTableSheetWorker(WorkBookContext workBookContext, SheetContext sheetContext) {
        super(sheetContext);
        this.workBookContext = workBookContext;
        this.sheetContext = sheetContext;
        Workbook workbook = sheetContext.getSheet().getWorkbook();
        pivotSheet = workbook.createSheet((sheetContext.getSheetNo() - 1) + "-" + sheetContext.getName());
        workbook.setSheetOrder(pivotSheet.getSheetName(), 0);
        workbook.setActiveSheet(0);
    }

    @Override
    public T call() {

        Stopwatch watch = Stopwatch.createStarted();
        super.call();
        Boolean rst = true;
        String md5 = null;
        logger = sheetContext.getCustomLogger();
        boolean log = sheetContext.getCustomLogger() != null;

        try {

            interrupted(sheetContext);
            //start to create sheet
            writeTable();

        } catch (Exception e) {
            if (sheetContext.getWrapper().getAction() == ActionEnum.MAIL) {
                MsgMailExcel msg = (MsgMailExcel) sheetContext.getWrapper().getMsg();
                msg.setDate(new Date());
                msg.setException(e);
            }
            if (log) {
                logger.error("Task({}) sheet worker(name:{}, sheetNo:{}, sheetName:{}) query error md5:{}",
                        sheetContext.getTaskKey(), sheetContext.getName(), sheetContext.getSheetNo(), sheetContext.getSheet().getSheetName(), md5);
                logger.error(e.toString(), e);
            }
            rst = false;
        }

        Object[] args = {sheetContext.getTaskKey(), sheetContext.getName(), md5, rst, sheetContext.getWrapper().getAction(), sheetContext.getWrapper().getxId(),
                sheetContext.getWrapper().getxUUID(), sheetContext.getSheetNo(), sheetContext.getSheet().getSheetName(), sheetContext.getDashboardId(),
                sheetContext.getWidgetId(), watch.elapsed(TimeUnit.MILLISECONDS)};
        if (log) {
            logger.info(
                    "Task({}) sheet worker({}) complete md5={}, status={}, action={}, xid={}, xUUID={}, sheetNo={}, sheetName={}, dashboardId={}, widgetId={}, cost={}ms",
                    args);
        }

        return (T) rst;
    }

    private void interrupted(SheetContext context) {
        if (Thread.interrupted()) {
            boolean log = context.getCustomLogger() != null;
            if (log) {
                logger.error("Task({}) sheet worker(name:{}, sheetNo:{}, sheetName:{}) interrupted",
                        context.getTaskKey(), context.getName(), context.getSheetNo(), context.getSheet().getSheetName());
            }
            throw new RuntimeException("Task(" + context.getTaskKey() + ") sheet worker(name:" + context.getName() + ", " +
                    "sheetNo:" + context.getSheetNo() + ", sheetName:" + context.getSheet().getSheetName() + ") interrupted");
        }
    }

    private Document getHTMLDocument() {
        String exportHtml = "";
        if (workBookContext != null && !workBookContext.getWidgets().isEmpty()) {
            List<WidgetContext> widgetContextList = workBookContext.getWidgets();
            WidgetContext widgetContext = widgetContextList.get(0);
            exportHtml = widgetContext.getExecuteParam().getExportHTML();
        }
        Document doc = null;
        if (!StringUtils.isEmpty(exportHtml)) {
            doc = Jsoup.parseBodyFragment(exportHtml);
        }
        return doc;
    }

    private void writeTable() {
        Elements tables = getHTMLDocument().getElementsByTag("table");
        Element leftTable = null, tableHeader = null, tableBody = null;
        for (Element table : tables) {
            if (!table.hasAttr("style")) {
                leftTable = table;
            } else {
                Elements tableHeaders = table.getElementsByTag("thead");
                if (tableHeaders.size() > 0) {
                    tableHeader = table;
                }
                Elements tableBodies = table.getElementsByTag("tbody");
                if (tableBodies.size() > 0) {
                    tableBody = table;
                }
            }
        }
        Integer rowKeyWidth = getRowKeyWidth(leftTable);
        Integer columnKeyHeight = getColumnKeyHeight(tableHeader);

        writeTableHeader(tableHeader, rowKeyWidth, columnKeyHeight);

        writeLeftTable(leftTable, rowKeyWidth, columnKeyHeight);

        writeTableBody(tableBody, rowKeyWidth, columnKeyHeight);

    }

    private void writeLeftTable(Element table, Integer rowKeyWidth, Integer columnKeyHeight) {
        Sheet sheet = pivotSheet;

        Elements rows = table.getElementsByTag("tr");
        int [] rowSpanIdx = new int[rowKeyWidth];
        for (int colIdx = 0; colIdx < rowKeyWidth; colIdx++) {
            rowSpanIdx[colIdx] = 1;
        }

        for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
            Elements th_s = rows.get(rowNum).getElementsByTag("th");
            int currentRowNum = rowNum + columnKeyHeight;
            Row excelRow = sheet.createRow(currentRowNum);
            if (th_s.size() == rowKeyWidth) {
                for (int colIdx = 0; colIdx < rowKeyWidth; colIdx++) {
                    Element th = th_s.get(colIdx);
                    if (th.hasAttr("rowspan")) {
                        rowSpanIdx[colIdx] = Integer.parseInt(th.attr("rowspan"));
                    }

                    Cell cell = excelRow.createCell(colIdx);
                    String cellValue = th.getAllElements().first().child(0).ownText();
                    cell.setCellValue(cellValue);

                    //merge cell
                    if (rowSpanIdx[colIdx] > 1) {
                        CellRangeAddress mergeRange = new CellRangeAddress(currentRowNum, currentRowNum + rowSpanIdx[colIdx] - 1, colIdx, colIdx);
                        
                        try {
                            sheet.addMergedRegion(mergeRange);
                        }
                        catch(Exception ex){
                            logger.error("Task({}) sheet worker(name:{}, sheetNo:{}, sheetName:{}), error writeLeftTable addMergedRegion, cell Value:{}",
                                    sheetContext.getTaskKey(), sheetContext.getName(), sheetContext.getSheetNo(), sheetContext.getSheet().getSheetName(), cellValue);
                            logger.error(ex.toString(), ex);
                        }
                    }
                }
            } else {
                int colIdx = 0;
                for (int idx = 0; idx < rowKeyWidth; idx++) {
                    if (rowSpanIdx[idx] > 1) {
                        continue;
                    } else {
                        Element th = th_s.get(colIdx);
                        Cell cell = excelRow.createCell(idx);
                        cell.setCellValue(th.getAllElements().first().child(0).ownText());
                        colIdx++;
                    }
                }
            }
        }
    }

    private void writeTableHeader(Element table, Integer rowKeyWidth, Integer columnKeyHeight) {
        Sheet sheet = pivotSheet;
        Elements rows = table.getElementsByTag("tr");
        for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
            Elements th_s = rows.get(rowNum).getElementsByTag("th");
            Row row = sheet.createRow(rowNum);
            int nextColNum = rowKeyWidth;
            for (Element th : th_s) {
                Element p = th.getAllElements().first();
                int colspan = 1;
                if (p.hasAttr("colspan")) {
                    colspan = Integer.parseInt(p.attr("colspan"));
                }
                Cell cell = row.createCell(nextColNum);
                
                String cellValue = p.child(0).ownText();
                cell.setCellValue(cellValue);

                if (colspan > 1) {
                    CellRangeAddress rangeAddress = new CellRangeAddress(rowNum,rowNum,nextColNum,(nextColNum + colspan - 1));
                    try {
                        sheet.addMergedRegion(rangeAddress);
                    }
                    catch(Exception ex){
                        logger.error("Task({}) sheet worker(name:{}, sheetNo:{}, sheetName:{}), error writeTableHeader addMergedRegion, cell Value:{}",
                                sheetContext.getTaskKey(), sheetContext.getName(), sheetContext.getSheetNo(), sheetContext.getSheet().getSheetName(), cellValue);
                        logger.error(ex.toString(), ex);
                    }
                }

                nextColNum = nextColNum + colspan;
            }
        }

        // merge left corner
        CellRangeAddress leftCorner = new CellRangeAddress(0, columnKeyHeight - 1, 0, rowKeyWidth - 1);

        try {
            sheet.addMergedRegion(leftCorner);
        }
        catch(Exception ex){
            logger.error("Task({}) sheet worker(name:{}, sheetNo:{}, sheetName:{}), Error merge leftCorner",
                    sheetContext.getTaskKey(), sheetContext.getName(), sheetContext.getSheetNo(), sheetContext.getSheet().getSheetName());
            logger.error(ex.toString(), ex);
        }
        
    }

    private void writeTableBody(Element table, Integer rowKeyWidth, Integer columnKeyHeight) {
        Sheet sheet = pivotSheet;
        Elements rows = table.getElementsByTag("tr");
        for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
            Elements td_s = rows.get(rowNum).getElementsByTag("td");
            int currentRowNum = columnKeyHeight + rowNum;
            Row excelRow = sheet.getRow(currentRowNum);
            for (int colIdx = 0; colIdx < td_s.size(); colIdx++) {
                Element td = td_s.get(colIdx);
                Elements p_s = td.getElementsByTag("p");
                Cell cell = excelRow.createCell(colIdx + rowKeyWidth);
                StringBuffer content = new StringBuffer();
                for (Element p : p_s) {
                    content.append(p.getAllElements().first().ownText());
                    content.append("\n");
                }
                cell.setCellValue(content.toString());
            }
        }
    }

    private Integer getRowKeyWidth(Element leftTable) {
        Integer rowKeyWidth = 0;
        Elements rowKeys = leftTable.select("thead > tr");
        for (Element tr : rowKeys) {
            Elements th = tr.getElementsByTag("th");
            rowKeyWidth = Math.max(th.size(), rowKeyWidth);
        }
        return rowKeyWidth;
    }

    private Integer getColumnKeyHeight(Element tableHeader) {
        Integer columnKeyHeight = 0;
        Elements columnKeys = tableHeader.getElementsByTag("tr");
        columnKeyHeight = Math.max(columnKeys.size(), columnKeyHeight);

        return columnKeyHeight;
    }

}
