package com.yupi.yuaicodemother.core;

import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
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

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode(
                "生成一个最简网页Demo，要求有内容、互动按钮等，不超过30行代码",
                CodeGenTypeEnum.MULTI_FILE,
                1L // 假设 appId 为 1
        );
        Assertions.assertNotNull(file, "生成的文件不应为 null");
    }

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
}