package com.example.batchservice.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
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

    @Scheduled(cron = "0 */1 * * * ?")
    public void scheduleJob() {
        try {
            jobLauncher.run(userNotificationJob, new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis()) // 고유 파라미터 추가
                    .toJobParameters());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

