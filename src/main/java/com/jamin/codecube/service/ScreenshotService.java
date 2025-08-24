package com.jamin.codecube.service;

public interface ScreenshotService {
    /**
     * 生成并上传网页截图
     * @param webUrl
     * @return
     */
    String generateAndUploadScreenshot(String webUrl);
}
