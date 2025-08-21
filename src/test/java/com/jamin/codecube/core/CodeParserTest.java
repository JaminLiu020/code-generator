package com.jamin.codecube.core;

import com.jamin.codecube.ai.model.HtmlCodeResult;
import com.jamin.codecube.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CodeParserTest {

    @Test
    void parseHtmlCode() {
        String codeContent = """
                随便写一段描述”
                ```html
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Simple Webpage</title>
                    <link rel="stylesheet" href="styles.css">
                </head>
                <body>
                    <h1>Welcome to My Simple Webpage</h1>
                    <p>This is a simple webpage with a button.</p>
                    <button id="myButton">Click Me!</button>
                
                    <script src="script.js"></script>
                </body>
                </html>
                ```
                结束
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                创建一个完整的网页：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>欢迎使用</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```
                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```
                ```js
                console.log('页面加载完成');
                ```
                文件创建完成！
                """;

        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHtmlCode());
        Assertions.assertNotNull(result.getCssCode());
        Assertions.assertNotNull(result.getJsCode());
    }





}