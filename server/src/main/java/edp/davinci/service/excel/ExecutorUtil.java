package edp.davinci.service.excel;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.concurrent.*;

@Slf4j
public class ExecutorUtil {

    public static final ExecutorService WORKBOOK_WORKERS = Executors.newFixedThreadPool(4,
            new ThreadFactoryBuilder().setNameFormat("Workbook-worker-%d").setDaemon(true).build());

    public static final ExecutorService SHEET_WORKERS = Executors.newFixedThreadPool(16,
            new ThreadFactoryBuilder().setNameFormat("Sheet-worker-%d").setDaemon(true).build());

    public static <T> Future<T> submitWorkbookTask(WorkBookContext context, Logger customLogger) {
        return ExecutorUtil.submitWorkbookTask(new WorkbookWorker(context), customLogger);
    }

    private static <T> Future<T> submitWorkbookTask(WorkbookWorker worker, Logger customLogger) {
        printThreadPoolStatusLog(WORKBOOK_WORKERS, "WORKBOOK_WORKERS", customLogger);
        return ExecutorUtil.WORKBOOK_WORKERS.submit(worker);
    }

    public static <T> Future<T> submitSheetTask(SheetContext context, Logger customLogger) {
        return ExecutorUtil.submitSheetTask(new SheetWorker(context), customLogger);
    }

    private static <T> Future<T> submitSheetTask(SheetWorker worker, Logger customLogger) {
        printThreadPoolStatusLog(SHEET_WORKERS, "SHEET_WORKERS", customLogger);
        return ExecutorUtil.SHEET_WORKERS.submit(worker);
    }

    public static <T> Future<T> submitSheetTask(WorkBookContext workBookContext, SheetContext context, Logger customLogger) {
        return ExecutorUtil.submitSheetTask(new PivotTableSheetWorker(workBookContext, context), customLogger);
    }

    private static <T> Future<T> submitSheetTask(PivotTableSheetWorker pivotTableSheetWorker, Logger customerLogger) {
        printThreadPoolStatusLog(SHEET_WORKERS, "SHEET_WORKERS", customerLogger);
        return ExecutorUtil.SHEET_WORKERS.submit(pivotTableSheetWorker);
    }

    public static void printThreadPoolStatusLog(ExecutorService executorService, String serviceName, Logger customLogger) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;
        Object[] args = {
                serviceName,
                executor.getKeepAliveTime(TimeUnit.SECONDS),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getTaskCount(),
                executor.getCompletedTaskCount()
        };
        if (customLogger != null) {
            customLogger.info("{} keep alive time:{}, poolSize:{}, waiting queue size:{}, task count:{}, completed task size:{}", args);
        }
    }
}
