package ru.pochta.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import ru.pochta.config.YmlProperties;
import ru.pochta.service.tasks.SftpImportTask;

@Service
@Slf4j
public class TaskRunnerServiceImpl implements TaskRunnerService {

    private final long fixedRateValue;
    private final boolean taskEnabled;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final SftpImportTask sftpImportTask;

    @Autowired
    public TaskRunnerServiceImpl(@Qualifier("serviceTaskScheduler") ThreadPoolTaskScheduler scheduler,
                                 SftpImportTask sftpImportTask,
                                 YmlProperties properties) {
        this.taskScheduler = scheduler;
        this.sftpImportTask = sftpImportTask;
        this.fixedRateValue = properties.getFixedRateValue() * 1000;
        this.taskEnabled = properties.isTaskEnabled();
    }

    @Override
    public void runTask() {
        System.out.println(this.taskScheduler);
        System.out.println(this.fixedRateValue);
        taskScheduler.scheduleAtFixedRate(
                sftpImportTask,
                fixedRateValue
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runTaskAfterStart() {
        if (this.taskEnabled) {
            log.info("Scheduled tasks runner enabled! Start running tasks...");
            runTask();
        } else {
            log.info("Scheduled tasks runner disabled!");
        }
    }

}
