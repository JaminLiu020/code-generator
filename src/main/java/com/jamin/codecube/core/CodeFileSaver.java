package com.jamin.codecube.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jamin.codecube.ai.model.HtmlCodeResult;
import com.jamin.codecube.ai.model.MultiFileCodeResult;
import com.jamin.codecube.model.enums.CodeGenTypeEnum;
import com.jamin.codecube.model.enums.FrontendFileTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Deprecated
public class CodeFileSaver {
    public static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output/";

    /**
     * 保存 HTML 代码结果到文件
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult){
        // 1. 构建唯一目录
        final String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        // 2. 保存文件
        WriteToFile(htmlCodeResult.getHtmlCode(), baseDirPath, FrontendFileTypeEnum.HTML.getFileName());
        // 3. 返回文件目录对象
        return new File(baseDirPath);
    }

    /**
     * 保存多文件代码结果到文件
     * @param multiFileCodeResult
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult) {
        // 1. 构建唯一目录
        final String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        // 2. 保存文件
        WriteToFile(multiFileCodeResult.getHtmlCode(), baseDirPath, FrontendFileTypeEnum.HTML.getFileName());
        WriteToFile(multiFileCodeResult.getCssCode(), baseDirPath, FrontendFileTypeEnum.CSS.getFileName());
        WriteToFile(multiFileCodeResult.getJsCode(), baseDirPath, FrontendFileTypeEnum.JS.getFileName());
        // 3. 返回文件目录对象
        return new File(baseDirPath);
    }

    /**
     * 构建唯一的目录路径：tmp/code_output/bizType_雪花ID
     * @param bizType
     * @return
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);

        return dirPath;
    }

    /**
     * 将代码写入文件
     * @param Code
     * @param dirPath
     * @param fileName
     */
    private static void WriteToFile(String Code, String dirPath, String fileName) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(Code, filePath, StandardCharsets.UTF_8);
    }
}
