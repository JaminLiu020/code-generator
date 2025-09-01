package com.jamin.codecube.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

import java.util.List;

public class RetryOutputGuardrail implements OutputGuardrail {

    private final List<String> genTypes = List.of("HTML", "MULTI_FILE", "VUE_PROJECT");

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String response = responseFromLLM.text();
        // 检查是否包含预期的生成类型
        for (String genType : genTypes) {
            if (response.equals(genType)) {
                return success();
            }
        }
        return reprompt("并未正确返回生成类型", "请按照System prompt的要求，确保响应中包含以下生成类型之一: " + String.join(", ", genTypes));
    }
//    @Override
//    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
//        String response = responseFromLLM.text();
//        // 检查响应是否为空或过短
//        if (response == null || response.trim().isEmpty()) {
//            return reprompt("响应内容为空", "请重新生成完整的内容");
//        }
//        if (response.trim().length() < 10) {
//            return reprompt("响应内容过短", "请提供更详细的内容");
//        }
//        // 检查是否包含敏感信息或不当内容
//        if (containsSensitiveContent(response)) {
//            return reprompt("包含敏感信息", "请重新生成内容，避免包含敏感信息");
//        }
//        return success();
//    }
    
    /**
     * 检查是否包含敏感内容
     */
    private boolean containsSensitiveContent(String response) {
        String lowerResponse = response.toLowerCase();
        String[] sensitiveWords = {
            "密码", "password", "secret", "token", 
            "api key", "私钥", "证书", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
