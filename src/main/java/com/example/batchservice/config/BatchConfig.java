package com.example.batchservice.config;

import com.example.batchservice.processor.EmailProcessor;
import com.example.batchservice.reader.EmailReader;
import com.example.batchservice.writer.EmailWriter;
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
    private final EmailReader emailReader;
    private final EmailProcessor emailProcessor;
    private final EmailWriter emailWriter;

    public BatchConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                       EmailReader emailReader, EmailProcessor emailProcessor, EmailWriter emailWriter) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.emailReader = emailReader;
        this.emailProcessor = emailProcessor;
        this.emailWriter = emailWriter;
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
                .<String, String>chunk(10) // 10개씩 묶어서 처리
                .reader(emailReader)
                .processor(emailProcessor)
                .writer(emailWriter)
                .build();
    }
}
