package com.jamin.codecube.ai;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class AiConcurrentTest {

    @Autowired
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Test
    public void testConcurrentRoutingCalls() throws InterruptedException {
        String[] prompts = {
                "做一个简单的HTML页面",
                "做一个多页面网站项目",
                "做一个Vue管理系统"
        };
        // 使用虚拟线程并发执行（原始版本：无计时）
        Thread[] threads = new Thread[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            final String prompt = prompts[i];
            final int index = i + 1;
            threads[i] = Thread.ofVirtual().start(() -> {
                AiCodeGenTypeRoutingService service = routingServiceFactory.createAiCodeGenTypeRoutingService();
                var result = service.routeCodeGenType(prompt);
                log.info("线程 {}: {} -> {}", index, prompt, result.getValue());
            });
        }
        // 等待所有任务完成
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    public void testConcurrentRoutingCalls_Timed() throws InterruptedException {
        String[] prompts = {
                "做一个简单的HTML页面",
                "做一个多页面网站项目",
                "做一个Vue管理系统"
        };
        // 全局计时（批次总耗时，含线程与服务创建）
        long globalStartNs = System.nanoTime();

        // 使用虚拟线程并发执行（计时版本）
        Thread[] threads = new Thread[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            final String prompt = prompts[i];
            final int index = i + 1;
            threads[i] = Thread.ofVirtual().start(() -> {
                // 先创建服务，不计入本线程执行耗时
                AiCodeGenTypeRoutingService service = routingServiceFactory.createAiCodeGenTypeRoutingService();
                long startNs = System.nanoTime();
                try {
                    var result = service.routeCodeGenType(prompt);
                    log.info("线程 {}: {} -> {}", index, prompt, result.getValue());
                } finally {
                    long costMs = (System.nanoTime() - startNs) / 1_000_000;
                    log.info("线程 {} 执行用时(不含服务创建): {}ms", index, costMs);
                }
            });
        }
        // 等待所有任务完成
        for (Thread thread : threads) {
            thread.join();
        }
        long totalMs = (System.nanoTime() - globalStartNs) / 1_000_000;
        log.info("并行任务总耗时(含创建与调度): {}ms", totalMs);
    }
}
