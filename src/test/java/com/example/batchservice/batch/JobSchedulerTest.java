package com.example.batchservice.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JobSchedulerTest {

    @Autowired
    JobScheduler jobScheduler;

    // 디버깅용
    @Test
    void scheduleJob() {
        jobScheduler.scheduleJob();
    }


}