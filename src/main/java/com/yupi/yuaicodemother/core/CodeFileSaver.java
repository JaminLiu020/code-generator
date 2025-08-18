package com.yupi.yuaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.yuaicodemother.ai.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.ai.model.enums.FrontendFileTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class CodeFileSaver {
    public static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output/";

    /**
     * 保存 HTML 代码结果到文件
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult){
        final String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        WriteToFile(htmlCodeResult.getHtmlCode(), baseDirPath, FrontendFileTypeEnum.HTML.getFileName());
        return new File(baseDirPath);
    }

    /**
     * 保存多文件代码结果到文件
     * @param multiFileCodeResult
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult) {
        final String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        WriteToFile(multiFileCodeResult.getHtmlCode(), baseDirPath, FrontendFileTypeEnum.HTML.getFileName());
        WriteToFile(multiFileCodeResult.getCssCode(), baseDirPath, FrontendFileTypeEnum.CSS.getFileName());
        WriteToFile(multiFileCodeResult.getJsCode(), baseDirPath, FrontendFileTypeEnum.JS.getFileName());
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
