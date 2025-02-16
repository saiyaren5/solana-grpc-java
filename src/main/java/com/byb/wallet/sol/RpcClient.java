package com.byb.wallet.sol;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.byb.wallet.abstracts.AbstractBaseHttpClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;


/*
 * RPC 客户端
 *
 * @author Byb Team
 * @since 2024-03-25  19:21
 */
@Slf4j
@Component
public class RpcClient extends AbstractBaseHttpClient {
    private static final OkHttpClient client;


    static {
        client = (new OkHttpClient.Builder())
                .connectTimeout(50L, TimeUnit.SECONDS)
                .readTimeout(50L, TimeUnit.SECONDS)
                .writeTimeout(50L, TimeUnit.SECONDS)
                .callTimeout(50L, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5000, 5, TimeUnit.MINUTES)).build();
    }

    /**
     * jsonRpc
     *
     * @param method 方法
     * @param params 参数
     * @return result 结果
     */
    public synchronized String jsonRpc(String method, List<Object> params) {
        JSONObject obj = JSONUtil.createObj().set("id", 1).set("jsonrpc", "2.0").set("method", method).set("params", params);
        String url = "https://go.getblock.io/22fd29113b5b4842a6d59e903d5d96e1";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            try {
                Response response = client.newCall((new Request.Builder()).url(url).post(RequestBody.create(MediaType.get("application/json"), obj.toString())).build()).execute();
                if (response.isSuccessful() & response.body() != null) {
                    //result = response.body().string();
                    // 获取响应体
                    ResponseBody body = response.body();
                    // 使用BufferedSource来流式读取响应数据
                    BufferedSource source = body.source();
                    byte[] buffer = new byte[8192]; // 8KB的缓冲区
                    int bytesRead;
                    // 持续读取，直到源数据完全读完
                    while ((bytesRead = source.read(buffer)) != -1) {
                        // 处理读取的数据块 (这里你可以做进一步的处理，比如保存到文件、分析等)
                        result.append(new String(buffer, 0, bytesRead));
                    }
                    if (result.length() > 0) {
                        return result.toString();
                    }
                }
                Thread.sleep(300);
            } catch (Exception e) {
                log.error("请求异常,url:{},参数：{},异常:{}", url, obj, e.getMessage());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {

                }
            }
        }
        return result.toString();
    }


    /**
     * 获取最新Solt
     *
     * @return solt
     */
    public Long getSlot() {
        String result = jsonRpc("getSlot", null);
        return StrUtil.isBlank(result) || result.contains("error") ? 0L : JSONUtil.parseObj(result).getLong("result");
    }

    /**
     * 根据区块高度获取区块
     *
     * @param slot 区块高度
     * @return 区块
     */

    public JSONObject getBlockBySlot(Long slot) {
//        String result = jsonRpc("getBlock", CollUtil.newArrayList(slot, JSONUtil.createObj().set("encoding", "json").set("transactionDetails", "accounts").set("rewards", false).set("maxSupportedTransactionVersion", 0)));
        String result = jsonRpc("getBlock", CollUtil.newArrayList(slot, JSONUtil.createObj().set("encoding", "jsonParsed").set("transactionDetails", "full").set("rewards", false).set("maxSupportedTransactionVersion", 0)));
        try {
//            return null;
            return StrUtil.isBlank(result) ? null : JSONUtil.parseObj(result, true);
        } catch (Exception e) {
            log.error("Slot：{},结果：{},JSON解析区块异常:", slot, result, e);
            return null;
        }
    }

    /**
     * 根据交易哈希获取交易信息
     *
     * @param txHash 交易哈希
     * @return 交易信息
     */

    public JSONObject getTransaction(String txHash) {
        for (int i = 0; i < 4; i++) {
            String resultStr = jsonRpc("getTransaction", CollUtil.newArrayList(txHash, JSONUtil.createObj().set("encoding", "jsonParsed").set("maxSupportedTransactionVersion", 0)));
            if (StrUtil.isBlank(resultStr) || resultStr.contains("error")) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            try {
                return JSONUtil.parseObj(resultStr).getJSONObject("result");
            } catch (Exception e) {
                log.error("交易哈希：{},JSON解析交易异常:", txHash, e);
            }
        }

        return null;
    }

}
