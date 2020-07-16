package ru.pochta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import ru.pochta.config.YmlProperties;
import ru.pochta.service.tasks.SftpImportTask;

@Service
public class TaskRunnerServiceImpl implements TaskRunnerService {

    private long fixedRateValue;
    private ThreadPoolTaskScheduler taskScheduler;
    private SftpImportTask sftpImportTask;

    @Autowired
    public TaskRunnerServiceImpl(@Qualifier("serviceTaskScheduler") ThreadPoolTaskScheduler scheduler,
                                 SftpImportTask sftpImportTask,
                                 YmlProperties properties) {
        this.taskScheduler = scheduler;
        this.sftpImportTask = sftpImportTask;
        this.fixedRateValue = properties.getFixedRateValue() * 1000;
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
        runTask();
    }

}
