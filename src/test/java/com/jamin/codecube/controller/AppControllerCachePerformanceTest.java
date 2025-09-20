package com.jamin.codecube.controller;

import com.jamin.codecube.common.BaseResponse;
import com.jamin.codecube.model.dto.app.AppQueryRequest;
import com.jamin.codecube.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AppController 缓存性能测试
 * 测试 listGoodAppVOByPage 接口使用缓存前后的性能差异
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
public class AppControllerCachePerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    private AppQueryRequest testRequest;

    @BeforeEach
    void setUp() {
        // 创建测试请求对象
        testRequest = new AppQueryRequest();
        testRequest.setPageNum(1);
        testRequest.setPageSize(10);

        // 清理缓存，确保测试环境干净
        clearCache();
    }

    /**
     * 清理缓存
     */
    private void clearCache() {
        if (cacheManager.getCache("good_app_page") != null) {
            cacheManager.getCache("good_app_page").clear();
            log.info("已清理缓存");
        }
    }

    /**
     * 测试接口响应时间 - 无缓存 vs 有缓存
     */
    @Test
    void testResponseTimeWithAndWithoutCache() {
        log.info("=== 开始测试接口响应时间（无缓存 vs 有缓存）===");

        // 第一次请求（无缓存）
        long startTime1 = System.currentTimeMillis();
        ResponseEntity<BaseResponse> response1 = callListGoodAppAPI();
        long endTime1 = System.currentTimeMillis();
        long timeWithoutCache = endTime1 - startTime1;

        log.info("第一次请求（无缓存）耗时: {}ms", timeWithoutCache);
        assert response1.getStatusCode().is2xxSuccessful();

        // 第二次请求（有缓存）
        long startTime2 = System.currentTimeMillis();
        ResponseEntity<BaseResponse> response2 = callListGoodAppAPI();
        long endTime2 = System.currentTimeMillis();
        long timeWithCache = endTime2 - startTime2;

        log.info("第二次请求（有缓存）耗时: {}ms", timeWithCache);
        assert response2.getStatusCode().is2xxSuccessful();

        // 计算性能提升
        double improvement = ((double) (timeWithoutCache - timeWithCache) / timeWithoutCache) * 100;
        log.info("缓存性能提升: {:.2f}%", improvement);

        // 多次测试取平均值
        testMultipleRequestsAverage();
    }

    /**
     * 多次测试取平均响应时间
     */
    private void testMultipleRequestsAverage() {
        log.info("=== 开始多次测试取平均响应时间 ===");
        int testCount = 10;

        // 测试无缓存场景
        clearCache();
        long totalTimeWithoutCache = 0;
        for (int i = 0; i < testCount; i++) {
            clearCache(); // 每次都清理缓存
            long startTime = System.currentTimeMillis();
            callListGoodAppAPI();
            long endTime = System.currentTimeMillis();
            totalTimeWithoutCache += (endTime - startTime);
        }
        long avgTimeWithoutCache = totalTimeWithoutCache / testCount;

        // 测试有缓存场景
        clearCache();
        callListGoodAppAPI(); // 预热缓存
        long totalTimeWithCache = 0;
        for (int i = 0; i < testCount; i++) {
            long startTime = System.currentTimeMillis();
            callListGoodAppAPI();
            long endTime = System.currentTimeMillis();
            totalTimeWithCache += (endTime - startTime);
        }
        long avgTimeWithCache = totalTimeWithCache / testCount;

        log.info("无缓存平均响应时间: {}ms", avgTimeWithoutCache);
        log.info("有缓存平均响应时间: {}ms", avgTimeWithCache);

        double improvement = ((double) (avgTimeWithoutCache - avgTimeWithCache) / avgTimeWithoutCache) * 100;
        log.info("缓存平均性能提升: {:.2f}%", improvement);
    }

    /**
     * 测试QPS (每秒查询数) - 并发测试
     */
    @Test
    void testQPSWithAndWithoutCache() throws InterruptedException {
        log.info("=== 开始测试QPS（并发测试）===");

        int threadCount = 20; // 增加并发线程数，更好地测试高并发场景
        int requestsPerThread = 20; // 增加每线程请求数，总计400个请求

        // 测试无缓存QPS
        double qpsWithoutCache = testQPS("无缓存", threadCount, requestsPerThread, false);

        // 测试有缓存QPS
        double qpsWithCache = testQPS("有缓存", threadCount, requestsPerThread, true);

        double qpsImprovement = ((qpsWithCache - qpsWithoutCache) / qpsWithoutCache) * 100;
        log.info("QPS提升: {:.2f}%", qpsImprovement);
    }

    /**
     * 执行QPS测试
     */
    private double testQPS(String testName, int threadCount, int requestsPerThread, boolean useCache) throws InterruptedException {
        if (!useCache) {
            clearCache();
        } else {
            // 预热缓存
            clearCache();
            callListGoodAppAPI();
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        // 启动并发请求
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        ResponseEntity<BaseResponse> response = callListGoodAppAPI();
                        if (response.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("请求异常", e);
                    errorCount.addAndGet(requestsPerThread);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程完成
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        long totalTime = endTime - startTime;
        int totalRequests = threadCount * requestsPerThread;
        double qps = (double) successCount.get() / (totalTime / 1000.0);

        log.info("{}测试结果:", testName);
        log.info("  总请求数: {}", totalRequests);
        log.info("  成功请求数: {}", successCount.get());
        log.info("  失败请求数: {}", errorCount.get());
        log.info("  总耗时: {}ms", totalTime);
        log.info("  QPS: {:.2f}", qps);

        return qps;
    }

    /**
     * 压力测试 - 持续时间测试
     */
    @Test
    void testSustainedLoad() throws InterruptedException {
        log.info("=== 开始压力测试（持续15秒）===");

        int threadCount = 8; // 增加线程数到8个
        long testDurationMs = 15000; // 延长到15秒

        // 预热缓存
        clearCache();
        callListGoodAppAPI();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicLong requestCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + testDurationMs;

        // 启动持续请求
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        ResponseEntity<BaseResponse> response = callListGoodAppAPI();
                        if (response.getStatusCode().is2xxSuccessful()) {
                            requestCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                        Thread.sleep(5); // 减少间隔，增加并发压力
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("请求异常", e);
                    }
                }
            });
        }

        Thread.sleep(testDurationMs + 1000); // 等待测试完成
        executor.shutdown();

        long actualDuration = System.currentTimeMillis() - startTime;
        double qps = (double) requestCount.get() / (actualDuration / 1000.0);

        log.info("压力测试结果:");
        log.info("  测试时长: {}ms", actualDuration);
        log.info("  成功请求数: {}", requestCount.get());
        log.info("  失败请求数: {}", errorCount.get());
        log.info("  平均QPS: {:.2f}", qps);
    }

    /**
     * 缓存命中率测试 - 测试不同请求参数的缓存效果
     */
    @Test
    void testCacheHitRate() throws InterruptedException {
        log.info("=== 开始测试缓存命中率 ===");

        // 清理缓存
        clearCache();

        // 测试相同参数的请求（应该命中缓存）
        log.info("测试相同参数请求的缓存命中:");
        testSameParameterRequests();

        // 测试不同参数的请求（不会命中缓存）
        log.info("测试不同参数请求的缓存效果:");
        testDifferentParameterRequests();
    }

    private void testSameParameterRequests() {
        int requestCount = 20;
        long totalTime = 0;

        for (int i = 0; i < requestCount; i++) {
            long startTime = System.currentTimeMillis();
            ResponseEntity<BaseResponse> response = callListGoodAppAPI();
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);

            if (i == 0) {
                log.info("  第{}次请求（缓存预热）耗时: {}ms", i + 1, endTime - startTime);
            } else if (i < 5 || i == requestCount - 1) {
                log.info("  第{}次请求（缓存命中）耗时: {}ms", i + 1, endTime - startTime);
            }
        }

        double avgTime = (double) totalTime / requestCount;
        log.info("  相同参数{}次请求平均耗时: {:.2f}ms", requestCount, avgTime);
    }

    private void testDifferentParameterRequests() {
        int[] pageSizes = {5, 10, 15, 20, 25};
        long totalTime = 0;

        for (int i = 0; i < pageSizes.length; i++) {
            AppQueryRequest request = new AppQueryRequest();
            request.setPageNum(1);
            request.setPageSize(pageSizes[i]);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AppQueryRequest> httpEntity = new HttpEntity<>(request, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<BaseResponse> response = restTemplate.postForEntity("/app/good/list/page/vo", httpEntity, BaseResponse.class);
            long endTime = System.currentTimeMillis();
            
            long requestTime = endTime - startTime;
            totalTime += requestTime;
            log.info("  pageSize={}的请求耗时: {}ms", pageSizes[i], requestTime);
        }

        double avgTime = (double) totalTime / pageSizes.length;
        log.info("  不同参数{}次请求平均耗时: {:.2f}ms", pageSizes.length, avgTime);
    }

    /**
     * 调用 listGoodAppVOByPage API
     */
    private ResponseEntity<BaseResponse> callListGoodAppAPI() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AppQueryRequest> request = new HttpEntity<>(testRequest, headers);

        return restTemplate.postForEntity("/app/good/list/page/vo", request, BaseResponse.class);
    }
}
