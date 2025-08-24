package com.jamin.codecube.service.impl;

import com.jamin.codecube.service.ScreenshotService;
import com.jamin.codecube.utils.WebScreenshotUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScreenshotServiceImplTest {
    @Autowired
    private ScreenshotService screenshotService;

    @Test
    void generateAndUploadScreenshot() {
        String testUrl = "https://www.baidu.com";
        String cosUrl = screenshotService.generateAndUploadScreenshot(testUrl);
        Assertions.assertNotNull(cosUrl);
    }
}