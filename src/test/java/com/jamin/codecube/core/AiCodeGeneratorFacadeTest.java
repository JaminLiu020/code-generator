package com.jamin.codecube.core;

import com.jamin.codecube.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    /**
     * 测试生成并保存 HTML/CSS/JS 代码
     */
    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode(
                "生成一个最简网页Demo，要求有内容、互动按钮等，不超过30行代码",
                CodeGenTypeEnum.MULTI_FILE,
                1L // 假设 appId 为 1
        );
        Assertions.assertNotNull(file, "生成的文件不应为 null");
    }

    /**
     * 测试生成并保存代码流
     */
    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "生成一个最简的日程表网站，要求有内容、增删改查等，不超过30行代码",
                CodeGenTypeEnum.MULTI_FILE,
                1L // 假设 appId 为 1
        );
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result, "结果列表不应为 null");
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

    /**
     * 测试生成 Vue 项目代码流
     */
    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "简单的任务记录网站，总代码量不超过 200 行",
                CodeGenTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}