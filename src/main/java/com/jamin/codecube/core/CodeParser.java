package com.jamin.codecube.core;

import com.jamin.codecube.ai.model.HtmlCodeResult;
import com.jamin.codecube.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class CodeParser {
    public static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    public static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    public static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:javascript|js)\\s*\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 解析 HTML 代码
     * @param codeContent
     * @return
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        // 提取 HTML 代码块
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            htmlCodeResult.setHtmlCode(htmlCode.trim());
        }
        else {
            htmlCodeResult.setHtmlCode(codeContent.trim());
        }
        return htmlCodeResult;
    }

    /**
     * 解析多文件代码
     * @param codeContent
     * @return
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent){
        MultiFileCodeResult multiFileCodeResult = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置代码到结果对象
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            multiFileCodeResult.setHtmlCode(htmlCode.trim());
        }
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            multiFileCodeResult.setCssCode(cssCode.trim());
        }
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            multiFileCodeResult.setJsCode(jsCode.trim());
        }
        return multiFileCodeResult;
    }

    /**
     * 根据正则表达式提取代码块
     * @param codeContent
     * @param codePattern
     * @return
     */
    private static String extractCodeByPattern(String codeContent, Pattern codePattern) {
        Matcher matcher = codePattern.matcher(codeContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
