package com.jamin.codecube.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * HTML 代码结果
 */
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    /**
     * HTML 代码
     */
    @Description("HTML代码")
    private String htmlCode;

    /**
     * 描述
     */
    @Description("生成代码的中文描述，专用词可以保留英文")
    private String description;
}