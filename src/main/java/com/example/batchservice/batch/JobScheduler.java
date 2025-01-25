package com.example.batchservice.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job userNotificationJob;

    public JobScheduler(JobLauncher jobLauncher, Job userNotificationJob) {
        this.jobLauncher = jobLauncher;
        this.userNotificationJob = userNotificationJob;
    }

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    public void scheduleJob() {
        try {
            jobLauncher.run(userNotificationJob, new org.springframework.batch.core.JobParameters());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

