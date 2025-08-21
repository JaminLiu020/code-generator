package com.jamin.codecube.ai;

import com.jamin.codecube.ai.model.HtmlCodeResult;
import com.jamin.codecube.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeGeneratorServiceTest {
    @Autowired
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("生成一个最简网页Demo，要求有内容、互动按钮等");
        System.out.println(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("生成一个最简网页Demo，要求有内容、互动按钮等");
        System.out.println(result);
    }

    @Test
    void testChat() {
        String result = aiCodeGeneratorService.testChat("请输出最大长度的文本，用字母A填充");
        System.out.println(result);
    }

    @Test
    void generateHtmlCodeStream() {
        aiCodeGeneratorService.generateHtmlCodeStream("生成一个最简网页Demo，要求有内容、互动按钮等")
                .subscribe(System.out::print);
    }


}