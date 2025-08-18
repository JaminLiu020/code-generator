package com.yupi.yuaicodemother.ai;

import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

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