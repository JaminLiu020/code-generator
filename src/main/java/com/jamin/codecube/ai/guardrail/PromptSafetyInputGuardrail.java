package com.jamin.codecube.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入审查护轨-输入内容安全检查
 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 敏感词列表
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "忽略之前的指令", "ignore previous instructions", "ignore above",
            "破解", "hack", "绕过", "bypass", "越狱", "jailbreak", "习近平",
            "法轮功", "六四", "藏独", "疆独", "台独", "反动", "暴力", "色情",
            "毒品", "赌博", "诈骗", "恐怖主义", "恐怖分子", "极端主义", "邪教",
            "自杀", "未成年人", "共产党", "宗教自由", "自残", "虐待", "剥削",
            "人口贩卖", "毒品", "假装你是管理员"
    );

    // 注入攻击模式
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:"),
            Pattern.compile("(?i)write\\s+code\\s+to\\s+(?:steal|hack|bypass|crack)"),
            Pattern.compile("(?i)generate\\s+content\\s+that\\s+(?:is|are)\\s+(?:illegal|inappropriate|harmful)"),
            Pattern.compile("(?i)忽略\\s+(?:之前|以上|所有)\\s+(?:指令|命令|提示|内容|要求)"),
            Pattern.compile("(?:忘记|无视)\\s+(?:所有|一切)\\s+(?:以上|之前)"),
            Pattern.compile("(?:假装|扮演|表现)\\s+(?:像|如果|你是)")

    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        // 检查输入长度
        if (input.length() > 1000) {
            return fatal("输入内容过长，不要超过 1000 字");
        }
        // 检查是否为空
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }
        // 检查敏感词
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return fatal("输入包含不当内容，请修改后重试");
            }
        }
        // 检查注入攻击模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }
        return success();
    }
}
