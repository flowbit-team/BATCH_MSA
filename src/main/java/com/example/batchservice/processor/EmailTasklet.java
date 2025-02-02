package com.example.batchservice.processor;

import com.example.batchservice.reader.EmailReader;
import com.example.batchservice.writer.EmailWriter;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Component
public class EmailTasklet implements Tasklet {

    private final EmailReader emailReader;
    private final EmailWriter emailWriter;
    private final EmailProcessor emailProcessor;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5); //  최대 5개의 병렬 실행 제한

    public EmailTasklet(EmailReader emailReader, EmailWriter emailWriter, EmailProcessor emailProcessor) {
        this.emailReader = emailReader;
        this.emailWriter = emailWriter;
        this.emailProcessor = emailProcessor;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        long startTime = System.currentTimeMillis();
        System.out.println("[EmailTasklet] 이메일 처리 시작...");

        List<String> emails = emailReader.readAll();
        System.out.println("[EmailTasklet] 총 이메일 수: " + emails.size());

        String emailTemplate = emailProcessor.generateEmailTemplate();
        System.out.println("[EmailTasklet] 이메일 템플릿 생성 완료!");

        List<Future<?>> futures = emails.stream()
                .map(email -> executorService.submit(() -> {
                    try {
                        emailWriter.write(List.of(email + "::" + emailTemplate));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }))
                .collect(Collectors.toList());

        // ✅ 모든 작업 완료 대기
        for (Future<?> future : futures) {
            try {
                future.get(); //  작업이 끝날 때까지 대기
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(executorService.isShutdown()){
            executorService.shutdown();
        }
        System.out.println("[EmailTasklet] 이메일 처리 완료. 총 소요 시간: " + (System.currentTimeMillis() - startTime) + "ms");

        return RepeatStatus.FINISHED;
    }
}
