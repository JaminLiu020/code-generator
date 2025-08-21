package com.jamin.codecube.model.enums;

import lombok.Getter;

@Getter
public enum FrontendFileTypeEnum {
    HTML("HTML文件", "index.html"),
    CSS("CSS文件", "style.css"),
    JS("JavaScript文件", "script.js");

    private final String name;
    private final String fileName;

    FrontendFileTypeEnum(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
    }

    /**
     * 根据文件名获取枚举
     * @param fileName
     * @return
     */
    public static FrontendFileTypeEnum getEnumByFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        for (FrontendFileTypeEnum fileType : FrontendFileTypeEnum.values()) {
            if (fileType.getFileName().equals(fileName)) {
                return fileType;
            }
        }
        return null;
    }

}
