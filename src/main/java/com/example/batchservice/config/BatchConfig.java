package com.example.batchservice.config;

import com.example.batchservice.processor.EmailTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EmailTasklet emailTasklet; // ✅ Tasklet 주입

    public BatchConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                       EmailTasklet emailTasklet) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.emailTasklet = emailTasklet;
    }

    @Bean
    public Job userNotificationJob() {
        return jobBuilderFactory.get("userNotificationJob")
                .start(emailNotificationStep())
                .build();
    }

    @Bean
    public Step emailNotificationStep() {
        return stepBuilderFactory.get("emailNotificationStep")
                .tasklet(emailTasklet) // Tasklet 사용 (청크 X)
                .build();
    }
}
