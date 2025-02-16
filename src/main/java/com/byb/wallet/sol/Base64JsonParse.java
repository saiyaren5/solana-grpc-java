package com.byb.wallet.sol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import java.io.IOException;

public class Base64JsonParse {
    public static void main(String[] args) {
        String jsonArrayStr = "{\"transaction\": [\"ARTzlBFPhx0wxPDlT9rXd6ycnI/nDDA5Wgs0eW9xBeKUlzZlmxYwG3i3A8wPaC/6Ia9FzOdsBDW79eQpaEIybgGAAQAGCYuSOaCA6JLh3xy9JHc0tXiQ4z8j4j527AOv9dUzylPQHHj2uZz1h8FGGFwIcpyR6348Dz4qeydIOFIenbNnCOB/fMSBRKkj4RGJiJ+PJ5FveFvRswbVe/R04fq2g638aQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAEedVb8jHAbu50xW7OaBUH/bGy3qP0jlECsc2iVrwTjwbd9uHXZaGT2cvhRs7reawctIXtX1s3kTqM9YV+/wCpjJclj04kifG7PRApFI4NgwtaE5na/xCEBI572Nvp+Fm0P/on9df2SnTAmx8pWHneSwmrNt/J3VFLMhqns4zl6JdQcfNxmZrrw9fMWWLFuYoWEoBrpcBbqWkOEM1KoyoGBQQABQJTYwEABAAJA6TfUwAAAAAABwYAAgAMAwYBAQUbBgABAgUMBQgFDgYLDQsJCgsLCwsLCwsLAQIAI+UXy5d6460qAQAAAAdkAAHpUrvL1QAAAJO95B0AAAAAECcABgMCAAABCQFgp665NkgVLhKPEvctSenHESElZNK24oLeXIMzCklKowPd4d8DCAEG\", \"base64\"]}";
        try {
            // 第一步：使用ObjectMapper将JSON字符串解析为Java对象（这里解析为通用的Map类型，方便获取内部字段值）
            ObjectMapper objectMapper = new ObjectMapper();
            java.util.Map<String, Object> jsonMap = objectMapper.readValue(jsonArrayStr, java.util.Map.class);

            // 第二步：从解析后的对象中获取名为"transaction"的数组
            Object transactionArrayObj = jsonMap.get("transaction");
            if (transactionArrayObj instanceof java.util.List) {
                java.util.List<?> transactionList = (java.util.List<?>) transactionArrayObj;
                if (!transactionList.isEmpty()) {
                    String base64EncodedData = (String) transactionList.get(0);
                    // 第三步：对Base64编码数据进行解码
                    base64EncodedData = "ARTzlBFPhx0wxPDlT9rXd6ycnI/nDDA5Wgs0eW9xBeKUlzZlmxYwG3i3A8wPaC/6Ia9FzOdsBDW79eQpaEIybgGAAQAGCYuSOaCA6JLh3xy9JHc0tXiQ4z8j4j527AOv9dUzylPQHHj2uZz1h8FGGFwIcpyR6348Dz4qeydIOFIenbNnCOB/fMSBRKkj4RGJiJ+PJ5FveFvRswbVe/R04fq2g638aQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAEedVb8jHAbu50xW7OaBUH/bGy3qP0jlECsc2iVrwTjwbd9uHXZaGT2cvhRs7reawctIXtX1s3kTqM9YV+/wCpjJclj04kifG7PRApFI4NgwtaE5na/xCEBI572Nvp+Fm0P/on9df2SnTAmx8pWHneSwmrNt/J3VFLMhqns4zl6JdQcfNxmZrrw9fMWWLFuYoWEoBrpcBbqWkOEM1KoyoGBQQABQJTYwEABAAJA6TfUwAAAAAABwYAAgAMAwYBAQUbBgABAgUMBQgFDgYLDQsJCgsLCwsLCwsLAQIAI+UXy5d6460qAQAAAAdkAAHpUrvL1QAAAJO95B0AAAAAECcABgMCAAABCQFgp665NkgVLhKPEvctSenHESElZNK24oLeXIMzCklKowPd4d8DCAEG";
                    byte[] decodedBytes = Base64.decodeBase64(base64EncodedData);
                    String decodedString = new String(decodedBytes, "UTF-8");
                    System.out.println(decodedString);

                    // 第四步：尝试将解码后的字符串再次解析为JSON对象（这里简单打印，实际应用中可根据需求进一步处理）
                    Object decodedJsonObj = objectMapper.readValue(decodedString, Object.class);
                    System.out.println(decodedJsonObj);
                } else {
                    System.err.println("'transaction'数组为空，无法获取Base64编码数据进行解析。");
                }
            } else {
                System.err.println("'transaction'字段不是数组类型，不符合预期结构。");
            }
        } catch (IOException e) {
            System.err.println("解析JSON或Base64解码过程中出现错误：");
            e.printStackTrace();
        }
    }
}