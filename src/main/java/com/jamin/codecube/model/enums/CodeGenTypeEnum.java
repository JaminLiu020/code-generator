package com.jamin.codecube.model.enums;

import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {
    /**
     * 单文件代码生成
     */
    HTML("原生HTML模式", "html"),

    /**
     * 多文件代码生成
     */
    MULTI_FILE("原生多文件格式", "multi_file");

    private final String text;
    private final String value;


    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (CodeGenTypeEnum codeGenType : CodeGenTypeEnum.values()) {
            if (codeGenType.getValue().equals(value)) {
                return codeGenType;
            }
        }
        return null;
    }
}
