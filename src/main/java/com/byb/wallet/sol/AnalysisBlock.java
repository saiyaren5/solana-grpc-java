package com.byb.wallet.sol;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.byb.wallet.infrastructure.common.block.sol.entity.TransactionRecordSol;
import com.byb.wallet.infrastructure.constant.InfrastructureConstants;
import com.byb.wallet.sol.service.TransactionRecordSolService;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.byb.wallet.infrastructure.common.masterChain.MastrChainIdAndIndexEnum.SOLANA;

/**
 * 解析区块
 *
 * @author Byb Team
 * @since 2024-03-27  18:54
 */
@Slf4j
@Component
public class AnalysisBlock {

    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE * 4, CORE_POOL_SIZE * 8, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    @Autowired
    private MongoTemplate mongoTemplate;
    @Resource
    private RpcClient rpcClient;
    @Value("${byb.rpc.master}")
    private Integer master;
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    @Resource(name = "redisTemplate")
    private SetOperations<String, String> setOperations;
    private LoadingCache<String, Set<String>> memberAddressCache;
    @Resource(name = "redisTemplate")
    private HashOperations<String, String, String> hashOperations;
    @Resource
    private TransactionRecordSolService transactionRecordSolService;


    private static void getFromAddrOrToAddr(JSONArray postTokenBalances, int i, Map<String, String> contractAddressMap, HashMap<String, String> fromAddressAndToAddressMap, String fromTokenAddress, JSONArray preTokenBalances) {
        postTokenBalances.forEach(s -> {
            JSONObject postTokenBalance = JSONUtil.parseObj(s);
            if (postTokenBalance.getInt("accountIndex").equals(i)) {
                fromAddressAndToAddressMap.put(fromTokenAddress, postTokenBalance.getStr("owner"));
                contractAddressMap.put(fromTokenAddress, postTokenBalance.getStr("mint"));
            }
        });
        if (StrUtil.isBlank(fromAddressAndToAddressMap.get(fromTokenAddress))) {
            preTokenBalances.forEach(s -> {
                JSONObject postTokenBalance = JSONUtil.parseObj(s);
                if (postTokenBalance.getInt("accountIndex").equals(i)) {
                    fromAddressAndToAddressMap.put(fromTokenAddress, postTokenBalance.getStr("owner"));
                    contractAddressMap.put(fromTokenAddress, postTokenBalance.getStr("mint"));
                }
            });
        }
    }

    /**
     * 设置Redis
     *
     */

    private void setContractAddressInRedis(HashMap<String, String> map) {
        if (CollUtil.isEmpty(map)) {
            return;
        }
        try {
            executorService.execute(() -> {
                // 添加到集合-保存redis
                map.forEach((contractAddress, s) -> {
                    try {
                        String redisKey = InfrastructureConstants.RedisKey.CURRENCY + SOLANA.getChainShortName();
                        if (!hashOperations.hasKey(redisKey, contractAddress)) {
                            hashOperations.put(redisKey, contractAddress, JSONUtil.createObj().set("decimals", s).toString());
                        }
                    } catch (Exception e) {
                        log.error("设置Redis合约地址失败:{}", e.getMessage());
                    }
                });
            });
        } catch (Exception e) {
            log.error("设置Redis合约地址------线程池异常:{}", e.getMessage());
        }
    }

    public static void main(String[] args) {

        JSONObject jsonObjectNull = new JSONObject("{\"aaa\":null}");
        System.out.println(jsonObjectNull.getStr("aaa") == null);


//        parseJsonToData(0L,new JSONObject(""));

    }

    public void parseJsonToData(Long slot, JSONObject transaction) {
//        System.out.println(transaction);
        JSONObject transactionObj = transaction;
        JSONArray accountKeysArrays = transaction
                .getJSONObject("transaction").getJSONObject("message").getJSONArray("accountKeys");

        String firstSignature =transaction.getJSONObject("transaction").getJSONArray("signatures").get(0).toString();
        log.info("parseJsonToDataDoing:{}",firstSignature);
        // 获取payer
        String payer = accountKeysArrays.getJSONObject(0).getStr("pubkey");
        // 获取所有accounts
//        accountKeysArrays.forEach(o->{
////            System.out.println(o);
//        });
        //解析preTokens和postTokens
        JSONObject owners = new JSONObject();
        Set<String> ownerMintSet = new HashSet<>();
        List<String> ownerList = new ArrayList<>();
//        ownerM.clear();
        transactionObj.getJSONObject("meta").getJSONArray("preTokenBalances").forEach(preToken->{
            ownerList.add(JSONUtil.parseObj(preToken).getStr("owner"));
            owners.set("PRES#"+JSONUtil.parseObj(preToken).getStr("owner")
                    +JSONUtil.parseObj(preToken).getStr("mint"),JSONUtil.parseObj(preToken));
            //获取所有index,排除index为 payer的owner，其他即为
        });
        //最终要入库的数据
        JSONArray ownerChanges = new JSONArray();
        transactionObj.getJSONObject("meta").getJSONArray("postTokenBalances").forEach(postToken->{
            if(Collections.frequency(ownerList,JSONUtil.parseObj(postToken).getStr("owner")) % 2 != 0){
                owners.remove("PRES#"+JSONUtil.parseObj(postToken).getStr("owner")
                        +JSONUtil.parseObj(postToken).getStr("mint"));
                return;
            }
            // 如果不包含So11111111111111111111111111111111111111112，也移除
            if(owners.get("PRES#"+JSONUtil.parseObj(postToken).getStr("owner") + "So11111111111111111111111111111111111111112") == null) {
                owners.remove("PRES#"+JSONUtil.parseObj(postToken).getStr("owner")
                        +JSONUtil.parseObj(postToken).getStr("mint"));
                return ;
            }
            owners.set("POST#"+JSONUtil.parseObj(postToken).getStr("owner")
                    +JSONUtil.parseObj(postToken).getStr("mint"),JSONUtil.parseObj(postToken));
            ownerMintSet.add(JSONUtil.parseObj(postToken).getStr("owner")
                    +JSONUtil.parseObj(postToken).getStr("mint"));

            // 计算sol111...增加还是减少，设置type为：buy/sell/addLiquidity/removeLiquidity
            // 计算精度后的数值进行保存
//            BigDecimal preUiTokenAmount = owners.getJSONObject("POST#"+JSONUtil.parseObj(postToken).getStr("owner")
//                    +JSONUtil.parseObj(postToken).getStr("mint"))
//                    .getJSONObject("uiTokenAmount").getBigDecimal("uiAmount");
        });

//        System.out.println("ownerMintSet:::"+ownerMintSet);
        ownerMintSet.forEach(ownerMint->{
            owners.get("PRES#"+ownerMint);
            //如果是payer或Sol11...，退出
//            if(ownerMint.contains("GjSUVZxCDKgtoENZvZLGNdbzNmgMbZzDsSYc6NaS8JC5")
            if(ownerMint.contains(payer)
                    || ownerMint.contains("So11111111111111111111111111111111111111112")
            || owners.getStr("PRES#"+ownerMint, "").equals("")) return;
//            System.out.println(ownerMint);
//            System.out.println(JSONUtil.parseObj(owners.get("PRES#"+ownerMint)));
            String ownerWSOLAddress = JSONUtil.parseObj(owners.get("PRES#"+ownerMint)).getStr("owner")+"So11111111111111111111111111111111111111112";

//            System.out.println("11111=====>"+ownerMint);
//            System.out.println("11111PRES=====>"+owners.get("PRES#"+ownerMint));
//            System.out.println(JSONUtil.parseObj(owners.get("PRES#"+ownerMint)));
//            System.out.println(JSONUtil.parseObj(owners.get("PRES#"+ownerMint)));

            BigDecimal preUiTokenAmountMint = JSONUtil.parseObj(owners.get("PRES#"+ownerMint))
                    .getJSONObject("uiTokenAmount")
                    .getBigDecimal("uiAmount", BigDecimal.ZERO);
            BigDecimal postUiTokenAmountMint = JSONUtil.parseObj(owners.get("POST#"+ownerMint))
                    .getJSONObject("uiTokenAmount")
                    .getBigDecimal("uiAmount", BigDecimal.ZERO);

            BigDecimal preUiTokenAmountWSOL = JSONUtil.parseObj(owners.get("PRES#"+ownerWSOLAddress))
                    .getJSONObject("uiTokenAmount")
                    .getBigDecimal("uiAmount", BigDecimal.ZERO);

            System.out.println("PostData:::"+JSONUtil.parseObj(owners.get("POST#"+ownerWSOLAddress)));
            if(owners.getStr("POST#" + ownerWSOLAddress, "").equals("")) return ;
            System.out.println(owners);
            BigDecimal postUiTokenAmountWSOL = JSONUtil.parseObj(owners.get("POST#"+ownerWSOLAddress))
                    .getJSONObject("uiTokenAmount")
                    .getBigDecimal("uiAmount", BigDecimal.ZERO);
            if(preUiTokenAmountMint == null) preUiTokenAmountMint = BigDecimal.ZERO;
            if(postUiTokenAmountMint == null) postUiTokenAmountMint = BigDecimal.ZERO;
            if(preUiTokenAmountWSOL == null) preUiTokenAmountWSOL = BigDecimal.ZERO;
            if(postUiTokenAmountWSOL == null) postUiTokenAmountWSOL = BigDecimal.ZERO;
            BigDecimal mintChange = postUiTokenAmountMint.subtract(preUiTokenAmountMint);
            BigDecimal wSOLChange = postUiTokenAmountWSOL.subtract(preUiTokenAmountWSOL);
            Integer type = 0;
            if(mintChange.compareTo(BigDecimal.ZERO) == 0) return;
            if(mintChange.compareTo(BigDecimal.ZERO) < 0) {
                type = 1;
            }
            JSONObject changeData = new JSONObject();
            changeData.set("firstSignature", firstSignature)
                    .set("slot", slot)
                    .set("type", type)
//                    .set("info", JSONUtil.parseObj(owners.get("PRES#"+ownerWSOLAddress)))
                    .set("wSOL",wSOLChange)
                    .set("mint",JSONUtil.parseObj(owners.get("PRES#"+ownerMint)).getStr("mint"))
                    .set("owner",JSONUtil.parseObj(owners.get("PRES#"+ownerMint)).getStr("owner"))
                    .set("mintChange",mintChange);
            ownerChanges.add(changeData);
        });
        //遍历owners

//        System.out.println(owners);
        log.info("swapData:{}",ownerChanges);
        if(ownerChanges.size()>0) {
            ownerChanges.forEach(one->{
                try {
                    mongoTemplate.insert(one,JSONUtil.parseObj(one).getStr("mint"));
                } catch (Exception e) {
                    log.error("Slot：{},Mongo入库异常:{},异常数据:{}", slot, e,one);
                }
            });
        }
    }

    public void parseSave(Long slot, JSONObject block) {

//        mongoTemplate.insert(block,"BlockTest");

        List<JSONObject> transactionList = block.getJSONArray("transactions").toList(JSONObject.class);
        log.info("区块Slot:{},--------------->>>>>>>>>交易数据大小：{}", slot, transactionList.size());

//        transactionList.forEach(t->{
//            System.out.println("解析之前的tx"+t);
//        });

        // 在Solana区块链中，Vote111111111111111111111111111111111111111 是用于验证节点投票交易（Vote Transaction）的程序ID。
        // 这种交易是由验证节点（Validator）提交的，用来表明它们对网络上特定槽（Slot）的确认。这些投票被用来达成网络共识，并帮助确定哪些交易应该被最终确认。
        transactionList = transactionList.parallelStream().filter(t -> JSONUtil.parseObj(t).getJSONObject("transaction").getJSONObject("message").getJSONArray("accountKeys").parallelStream().noneMatch(ac -> JSONUtil.parseObj(ac).getStr("pubkey").equals("Vote111111111111111111111111111111111111111"))).map(JSONUtil::parseObj).collect(Collectors.toList());


        if (CollUtil.isEmpty(transactionList)) {
            log.info("区块Slot:{},  过滤之后的交易数据为空！", slot);
            return;
        }
        //setReceiptsInRedis(slot, transactionList);
        Long blockHeight = block.getLong("blockHeight");
        log.info("区块Slot:{},过滤Vote1111111.........后的交易数据大小：{}", slot, transactionList.size());
        HashMap<String, TransactionRecordSol> transactionSaveMap = new HashMap<>(8);
        transactionList.parallelStream().forEach( tx-> {
            try {
                String firstSignature = tx.getJSONObject("transaction").getJSONArray("signatures").get(0).toString();
                log.info("transactionForParseJsonToData:{},slot:{}", firstSignature, slot);
                parseJsonToData(slot, tx);
            } catch (Exception e) {
                log.info("transactionData:{}", tx);
                log.info("解析区块parallelStream异常:", e.getMessage());
            }
            if(1==1) return ;

            JSONObject transaction = tx.getJSONObject("transaction");
            JSONArray accountKeys = transaction.getJSONArray("accountKeys");
            String txHash = transaction.getJSONArray("signatures").get(0).toString();
            JSONObject txMeta = tx.getJSONObject("meta");
            mongoTemplate.insert(new JSONObject().set("txMeta", txMeta),"txMetaTest");

            // 交易成功，这里进行preTokens和postTokens的解析
            if(null == txMeta.getStr("err")){
                System.out.println("err====>"+txMeta.getStr("err"));

                JSONArray postTokenBalancesTxMeta = txMeta.getJSONArray("postTokenBalances");
                JSONArray preTokenBalances = txMeta.getJSONArray("preTokenBalances");

                String mintOwnerAmount = "";
                //遍历，必须含有sol1111...2，且So11111111111111111111111111111111111111112的数值前后发生了变化
                //拼接成：mint#owner#programId#amount
                Map<String, BigInteger> maps = new HashMap<>();
                Map<String, Integer> ownerCountMap = new HashMap<>();
                Map<String, String> ownerToken = new HashMap<>();
                Map<String, BigInteger> ownerTokenAmountMap = new HashMap<>();
                if(CollUtil.isNotEmpty(preTokenBalances)) {
                    preTokenBalances.forEach(s->{
                        JSONObject preTokenBalance = JSONUtil.parseObj(s);
                        BigInteger amount = preTokenBalance.getJSONObject("uiTokenAmount").getBigInteger("amount");
                        String mint = preTokenBalance.getStr("mint");
                        String owner = preTokenBalance.getStr("owner");
                        String programId = preTokenBalance.getStr("programId");
                        if(ownerCountMap.containsKey(owner)){
                            ownerCountMap.put(owner,ownerCountMap.get(owner)+1);
                        } else {
                            ownerCountMap.put(owner,1);
                        }
                        ownerTokenAmountMap.put(owner+"#"+mint, amount);
                        if(mint.contains("So11111111111111111111111111111111111111112")) {
                            String mintOwnerAmountStr = mint+"#"+owner+"#"+programId;
                            maps.put("pre"+mintOwnerAmountStr, amount);
                        }
                    });

//                    postTokenBalancesTxMeta.stream()

                    if(!maps.isEmpty()) {
                        System.out.println("hash=====>"+txHash);
                        postTokenBalancesTxMeta.forEach(o->{

                            JSONObject postTokenBalance = JSONUtil.parseObj(o);
                            BigInteger amount = postTokenBalance.getJSONObject("uiTokenAmount").getBigInteger("amount");
                            String mint = postTokenBalance.getStr("mint");
                            String owner = postTokenBalance.getStr("owner");
                            String programId = postTokenBalance.getStr("programId");

                            //如果owner不是拥有pair记录，或者没有So111...主币，或者数值没有变化则不是dex池子swap交易，直接退出
                            if(ownerCountMap.get(owner) != 2) return ;

                            if(mint.contains("So11111111111111111111111111111111111111112")) {
                                System.out.println("ownerCountMap===>"+ownerCountMap.get(owner));
                                String mintOwnerAmountStr = mint+"#"+owner+"#"+programId;
                                if(maps.get("pre"+mintOwnerAmountStr) == null) {
                                    System.out.println("preNull====>"+mintOwnerAmountStr);
                                    return ;
                                }
                                if(maps.get("pre"+mintOwnerAmountStr).compareTo(amount) != 0) {
                                    System.out.println("sol111，PRE===>"+maps.get("pre"+mintOwnerAmountStr));
                                    System.out.println("sol111，POST===>"+amount);
                                    maps.put("post"+mintOwnerAmountStr, amount);
                                }
                            } else {

                            }
                        });
                    }
                }

                // 解析交易数据，入库

                mongoTemplate.insert(new JSONObject().set("transaction", transaction),"KLineTest");
//                mongoTemplate.insert(new JSONObject().set("aa", "test1"),"CoinThumbTest");

            } else {
//                System.out.println("err====>null"+txMeta.getStr("err"));
                return ;
            }

            HashMap<String, String> contractAddressMapRedis = new HashMap<>(8);
            JSONArray postTokenBalancesTxMeta = txMeta.getJSONArray("postTokenBalances");
            if (CollUtil.isNotEmpty(postTokenBalancesTxMeta)) {
                postTokenBalancesTxMeta.forEach(s -> {
                    JSONObject postTokenBalance = JSONUtil.parseObj(s);
                    String mint = postTokenBalance.getStr("mint");
                    String decimals = postTokenBalance.getJSONObject("uiTokenAmount").getStr("decimals");
                    contractAddressMapRedis.put(mint, decimals);
                });
            }
            JSONArray preTokenBalancesTxMeta = txMeta.getJSONArray("preTokenBalances");
            if (CollUtil.isNotEmpty(preTokenBalancesTxMeta)) {
                preTokenBalancesTxMeta.forEach(s -> {
                    JSONObject preTokenBalance = JSONUtil.parseObj(s);
                    String mint = preTokenBalance.getStr("mint");
                    String decimals = preTokenBalance.getJSONObject("uiTokenAmount").getStr("decimals");
                    contractAddressMapRedis.put(mint, decimals);
                });
            }

            setContractAddressInRedis(contractAddressMapRedis);

            // 匹配地址
            boolean match = accountKeys.stream().filter(aks -> {
                JSONObject entries = JSONUtil.parseObj(aks);
                return !entries.getStr("pubkey").contains("111111111111111") && (entries.getBool("writable", true) || entries.getBool("signer", true));
            }).map(aks -> JSONUtil.parseObj(aks).getStr("pubkey")).anyMatch(this::matchLocalAddress);
            // 匹配地址onwer
            if (!match) {
                JSONArray postTokenBalancesMeta = txMeta.getJSONArray("postTokenBalances");
                match = postTokenBalancesMeta.stream().map(s -> JSONUtil.parseObj(s).getStr("owner")).filter(StrUtil::isNotBlank).distinct().anyMatch(this::matchLocalAddress);
                if (!match) {
                    JSONArray preTokenBalancesMeta = txMeta.getJSONArray("preTokenBalances");
                    match = preTokenBalancesMeta.stream().map(s -> JSONUtil.parseObj(s).getStr("owner")).filter(StrUtil::isNotBlank).distinct().anyMatch(this::matchLocalAddress);
                }
            }

            if (!match) {
                log.info("区块Slot:{} 交易哈希：{} 未匹配到本地地址，跳过！", slot, txHash);
                return;
            }

            if(1 == 1) return ;


            //log.info("区块Slot:{} 交易哈希：{} 匹配到本地地址，开始解析！", slot, txHash);
            JSONObject transactionDetailsInfo = rpcClient.getTransaction(txHash);

            //TODO  重试3次之后还是获取不到怎么处理？
            if (transactionDetailsInfo == null || transactionDetailsInfo.isEmpty()) {
                log.info("区块Slot:{} 交易哈希：{} 获取交易详情失败，跳过！", slot, txHash);
                return;
            }

            String blockHash = block.getStr("blockhash");
            DateTime transactionTime = null;
            try {
                Long blockTime = transactionDetailsInfo.getLong("blockTime");
                transactionTime = DateUtil.date(blockTime * 1000);
            } catch (Exception e) {
                log.error("交易哈希：{} 获取交易时间失败！", txHash);
            }

            JSONObject meta = transactionDetailsInfo.getJSONObject("meta");
            String computeUnitsConsumed = meta.getStr("computeUnitsConsumed");
            String fee = meta.getStr("fee");
            int status = meta.getJSONObject("status").containsKey("Ok") ? 1 : 0;

            JSONObject detailsInfoJSONObject = transactionDetailsInfo.getJSONObject("transaction");
            String version = transactionDetailsInfo.getStr("version");
            JSONObject message = detailsInfoJSONObject.getJSONObject("message");
            String signer = JSONUtil.parseObj(message.getJSONArray("accountKeys").get(0).toString()).getStr("pubkey");

            JSONArray innerInstructionsMeta = meta.getJSONArray("innerInstructions");
            DateTime finalTransactionTime = transactionTime;

            JSONArray instructions = message.getJSONArray("instructions");
            if (CollUtil.isNotEmpty(instructions)) {
                instructions.forEach(instruction -> {
                    JSONObject instructionJSONObject = JSONUtil.parseObj(instruction);
                    String programId = instructionJSONObject.getStr("programId");
                    String program = instructionJSONObject.getStr("program");
                    // 系统程序
                    if ("11111111111111111111111111111111".equalsIgnoreCase(programId)) {
                        JSONObject parsed = instructionJSONObject.getJSONObject("parsed");
                        if (parsed != null && !parsed.isEmpty()) {
                            String type = parsed.getStr("type");
                            if (StrUtil.isNotBlank(type)) {
                                // SOL转账
                                if ("transfer".equalsIgnoreCase(type)) {
                                    JSONObject parsedInfo = parsed.getJSONObject("info");
                                    buildSolTransactionData(parsedInfo, transactionSaveMap, slot, blockHeight, blockHash, txHash, finalTransactionTime, fee, computeUnitsConsumed, status, version, signer, program);
                                    //} else if ("create_account".equalsIgnoreCase(type)) {
                                    // TODO 示例 后续看是否需要解析
                                    //  "parsed": {
                                    //    "info": {
                                    //      "lamports": 2039280,
                                    //      "newAccount": "GEQLVxWrqyNFysX2yo3ndw33TsZgixyguDhkGCWwWxF3",
                                    //      "owner": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                                    //      "source": "BfktaV82c44Rb8MjsEwKnupp6AtL8KCM2iUH528jKEuk",
                                    //      "space": 165
                                    //    },
                                    //    "type": "createAccount"
                                    //  },
                                    //  "program": "system",
                                    //  "programId": "11111111111111111111111111111111",
                                    //  "stackHeight": 2
                                }
                            }
                        }
                    }

                    // 在Solana网络中，"programId": "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"代表的是Associated Token Program的ID。
                    // 这个程序的主要作用是帮助创建并管理与特定Solana钱包相关
                    else if ("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL".equalsIgnoreCase(programId)
                            || "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA".equalsIgnoreCase(programId)) {
                        JSONObject parsed = instructionJSONObject.getJSONObject("parsed");
                        if (parsed != null && !parsed.isEmpty()) {
                            String type = parsed.getStr("type");
                            if (StrUtil.isNotBlank(type)) {
                                // 代币转账
                                // 在Solana区块链中，"type": "transferChecked"是Token Program的一种指令类型，用于在特定的代币账户之间进行转账操作，并且会执行额外的安全性检查。
                                // transferChecked指令相对于普通的transfer指令来说，多了一些验证步骤，以确保代币的精度和数量符合预期。
                                if ("transferChecked".equalsIgnoreCase(type) || "transfer".equalsIgnoreCase(type)) {
                                    JSONObject parsedInfo = parsed.getJSONObject("info");
                                    String fromTokenAddress = parsedInfo.getStr("source");
                                    String toTokenAddress = parsedInfo.getStr("destination");
                                    String contractAddress = parsedInfo.getStr("mint");

                                    HashMap<String, String> fromAddressAndToAddressMap = new HashMap<>(8);
                                    Map<String, String> contractAddressMap = new HashMap<>(8);
                                    JSONArray postTokenBalances = meta.getJSONArray("postTokenBalances");
                                    JSONArray preTokenBalances = meta.getJSONArray("preTokenBalances");
                                    for (int i = 0; i < accountKeys.size(); i++) {
                                        if (JSONUtil.parseObj(accountKeys.get(i)).getStr("pubkey").equalsIgnoreCase(fromTokenAddress)) {
                                            getFromAddrOrToAddr(postTokenBalances, i, contractAddressMap, fromAddressAndToAddressMap, fromTokenAddress, preTokenBalances);
                                        } else if (JSONUtil.parseObj(accountKeys.get(i)).getStr("pubkey").equalsIgnoreCase(toTokenAddress)) {
                                            getFromAddrOrToAddr(postTokenBalances, i, contractAddressMap, fromAddressAndToAddressMap, toTokenAddress, preTokenBalances);
                                        }
                                    }

                                    if (StrUtil.isBlank(contractAddress)) {
                                        contractAddress = contractAddressMap.get(fromTokenAddress);
                                    }

                                    String fromAddress = null;
                                    if (CollUtil.isEmpty(fromAddressAndToAddressMap) || !fromAddressAndToAddressMap.containsKey(fromTokenAddress)) {
                                        log.error("创建代币账户并转账，获取from-owner地址为空，交易哈希：{}，fromTokenAddress：{}", txHash, fromTokenAddress);
                                        fromAddress = fromTokenAddress;
                                    } else {
                                        fromAddress = fromAddressAndToAddressMap.get(fromTokenAddress);
                                    }

                                    String toAddress = null;
                                    if (CollUtil.isEmpty(fromAddressAndToAddressMap) || !fromAddressAndToAddressMap.containsKey(toTokenAddress)) {
                                        log.error("创建代币账户并转账，获取from-owner地址为空，交易哈希：{}，toTokenAddress：{}", txHash, toTokenAddress);
                                        toAddress = toTokenAddress;
                                    } else {
                                        toAddress = fromAddressAndToAddressMap.get(toTokenAddress);
                                    }

                                    //setContractAddressInRedis(contractAddressMap.values().stream().distinct().collect(Collectors.toList()));

                                    if (!matchLocalAddress(fromAddress) && !matchLocalAddress(toAddress)) {
                                        return;
                                    }
                                    setCacheByReceiveCurrency(toAddress, contractAddress);
                                    String value = "0";
                                    if ("transfer".equalsIgnoreCase(type)) {
                                        value = parsedInfo.getStr("amount");
                                    } else if ("transferChecked".equalsIgnoreCase(type)) {
                                        value = parsedInfo.getJSONObject("tokenAmount").getStr("amount");
                                    }
                                    transactionSaveMap.put(txHash + "-" + fromAddress + "-" + toAddress + "-" + value + "-" + contractAddress, new TransactionRecordSol(txHash, blockHash, slot, blockHeight, computeUnitsConsumed, fee, fromAddress, fromTokenAddress, toAddress, toTokenAddress, value, status, contractAddress, finalTransactionTime, program, 1, signer, version));
                                }
                                // create
                                // closeAccount

                            }
                        }
                    }
                });

            }

            if (CollUtil.isNotEmpty(innerInstructionsMeta)) {
                innerInstructionsMeta.forEach(inner -> {
                    JSONObject innerJSONObject = JSONUtil.parseObj(inner);
                    JSONArray instructionsJsonArray = innerJSONObject.getJSONArray("instructions");
                    instructionsJsonArray.forEach(s -> {
                        JSONObject instruction = JSONUtil.parseObj(s);
                        String program = instruction.getStr("program");
                        String programId = instruction.getStr("programId");
                        // 系统
                        if ("system".equalsIgnoreCase(program) && "11111111111111111111111111111111".equalsIgnoreCase(programId)) {
                            JSONObject parsed = instruction.getJSONObject("parsed");
                            String type = parsed.getStr("type");
                            // SOL交易
                            if ("transfer".equalsIgnoreCase(type)) {
                                JSONObject parsedInfo = parsed.getJSONObject("info");
                                buildSolTransactionData(parsedInfo, transactionSaveMap, slot, blockHeight, blockHash, txHash, finalTransactionTime, fee, computeUnitsConsumed, status, version, signer, program);
                            }
                            // 创建账户
                            else if ("createAccount".equalsIgnoreCase(type)) {

                                // 1、A账户当转账 solana token1给B地址时，
                                //
                                //2、如果B地址没有和token1作为一对创建过关联账户；
                                //
                                //3、则A账户在这笔转账中，转账Token1的同时，再付出一笔0.002的SOL去创建B地址+token1创建关联账户，同时这0.002sol转入到该关联账户；
                                //
                                //4、该关联账户里质押的0.002sol，可以通过某种方法取出来，但是取出来后里面的所有token1币全部销毁；
                                //
                                //5、下次再转给B地址 token1的时候 依然需要创建关联账户；


                                // 转账solana token的时候：
                                //
                                //创建solana账户---转入token关联账户0.002sol的记录
                                //
                                //也要显示出来

                                // 转入转出都是同一个地址
                                JSONObject parsedInfo = parsed.getJSONObject("info");
                                String value = parsedInfo.getStr("lamports");
                                String toTokenAddres = parsedInfo.getStr("newAccount");
                                String fromAddress = parsedInfo.getStr("source");
                                String toAddress = fromAddress;
                                transactionSaveMap.put(txHash + "-" + fromAddress + "-" + toAddress + "-" + value, new TransactionRecordSol(txHash, blockHash, slot, blockHeight, computeUnitsConsumed, fee, fromAddress, null, toAddress, toTokenAddres, value, status, null, finalTransactionTime, program, 0, signer, version));
                            }
                        }
                        // 合约交易
                        else if ("spl-token".equalsIgnoreCase(program) &&
                                "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA".equalsIgnoreCase(programId)) {
                            JSONObject parsed = instruction.getJSONObject("parsed");
                            String type = parsed.getStr("type");
                            // 获取合约地址
                            if ("transfer".equalsIgnoreCase(type)) {
                                Map<String, String> contractAddressMap = new HashMap<>(8);
                                Map<String, String> ownerFromAddressMap = new HashMap<>(8);
                                JSONArray preTokenBalances = meta.getJSONArray("preTokenBalances");
                                preTokenBalances.parallelStream().forEach(preTokenBalance -> {
                                    JSONObject parseObj = JSONUtil.parseObj(preTokenBalance);
                                    Integer accountIndex = parseObj.getInt("accountIndex");
                                    String contractAddress = parseObj.getStr("mint");
                                    String owner = parseObj.getStr("owner");
                                    String address = JSONUtil.parseObj(message.getJSONArray("accountKeys").get(accountIndex)).getStr("pubkey");
                                    contractAddressMap.put(address, contractAddress);
                                    ownerFromAddressMap.put(address, owner);
                                });

                                Map<String, String> ownerToAddressMap = new HashMap<>();
                                JSONArray postTokenBalances = meta.getJSONArray("postTokenBalances");
                                postTokenBalances.parallelStream().forEach(postTokenBalance -> {
                                    JSONObject parseObj = JSONUtil.parseObj(postTokenBalance);
                                    Integer accountIndex = parseObj.getInt("accountIndex");
                                    String contractAddress = parseObj.getStr("mint");
                                    String owner = parseObj.getStr("owner");
                                    String address = JSONUtil.parseObj(message.getJSONArray("accountKeys").get(accountIndex)).getStr("pubkey");
                                    contractAddressMap.put(address, contractAddress);
                                    ownerToAddressMap.put(address, owner);
                                });

                                //setContractAddressInRedis(contractAddressMap.values().stream().distinct().collect(Collectors.toList()));

                                JSONObject parsedInfo = parsed.getJSONObject("info");
                                String fromTokenAddress = parsedInfo.getStr("source");
                                String toTokenAddress = parsedInfo.getStr("destination");
                                String contractAddress = contractAddressMap.get(fromTokenAddress);

                                String fromAddress;
                                if (CollUtil.isEmpty(ownerFromAddressMap) || ownerFromAddressMap.get(fromTokenAddress) == null) {
                                    fromAddress = fromTokenAddress;
                                    fromTokenAddress = null;
                                } else {
                                    fromAddress = ownerFromAddressMap.get(fromTokenAddress);
                                }

                                String toAddress;
                                if (CollUtil.isEmpty(ownerToAddressMap) || ownerToAddressMap.get(toTokenAddress) == null) {
                                    toAddress = toTokenAddress;
                                    toTokenAddress = null;
                                } else {
                                    toAddress = ownerToAddressMap.get(toTokenAddress);
                                }

                                if (!matchLocalAddress(fromAddress) && !matchLocalAddress(toAddress)) {
                                    return;
                                }
                                setCacheByReceiveCurrency(toAddress, contractAddress);
                                String value = parsedInfo.getStr("amount");
                                // 这里涉及到买入卖出的区别
                                //boolean swapFlag = meta.getJSONArray("logMessa ges").contains("Program log: Instruction: Buy") || StrUtil.isBlank(contractAddress);
                                boolean swapFlag = StrUtil.isBlank(contractAddress);
                                // type: 0-Sol Transfer-1-Contract Transfer-2-Swap Transfer
                                Integer typeSave = swapFlag ? 2 : 1;
                                transactionSaveMap.put(txHash + "-" + fromAddress + "-" + toAddress + "-" + value + "-" + contractAddress, new TransactionRecordSol(txHash, blockHash, slot, blockHeight, computeUnitsConsumed, fee, fromAddress, fromTokenAddress, toAddress, toTokenAddress, value, status, contractAddress, finalTransactionTime, program, typeSave, signer, version));
                            }
                            // getAccountDataSize
                            // initializeImmutableOwner
                            // initializeAccount3
                            // 授权
                        }

                    });

                });
            }
        });

        if (CollUtil.isNotEmpty(transactionSaveMap) && CollUtil.isNotEmpty(transactionSaveMap.values())) {
            transactionRecordSolService.saveBatch(transactionSaveMap.values());
        }
    }

    private void setReceiptsInRedis(Long slot, List<JSONObject> transactionList) {
        //CompletableFuture.runAsync(() -> {
        //    do try {
        //        String key = "block:receipts:" + SOLANA.getChainShortName();
        //        hashOperations.put(key, slot.toString(), JSONUtil.toJsonStr(transactionList));
        //        break;
        //    } catch (Exception e) {
        //        log.error("SOL-SetReceiptsInRedis error:", e);
        //    }
        //    while (true);
        //}, executorService);


    }

    private void buildSolTransactionData(JSONObject parsedInfo, HashMap<String, TransactionRecordSol> transactionSaveMap, Long slot, Long blockHeight, String blockhash, String txHash, DateTime transactionTime, String fee, String computeUnitsConsumed, int status, String version, String signer, String program) {
        String fromAddress = parsedInfo.getStr("source");
        String toAddress = parsedInfo.getStr("destination");
        if (!matchLocalAddress(fromAddress) && !matchLocalAddress(toAddress)) {
            return;
        }
        String value = parsedInfo.getStr("lamports");
        transactionSaveMap.put(txHash + "-" + fromAddress + "-" + toAddress + "-" + value, new TransactionRecordSol(txHash, blockhash, slot, blockHeight, computeUnitsConsumed, fee, fromAddress, null, toAddress, null, value, status, null, transactionTime, program, 0, signer, version));
    }

    private Boolean matchLocalAddress(String address) {
        Set<String> addressSet = null;
        try {
            if (memberAddressCache != null) {
                log.info("memberAddresses>>>>>>>address, {}", address);
                addressSet = memberAddressCache.get(SOLANA.getChainShortName());
            } else {
                memberAddressCache = Caffeine.newBuilder().maximumSize(1) // 设置最大缓存项数量
                        .expireAfterWrite(10, TimeUnit.SECONDS) // 写入后的过期时间
                        .refreshAfterWrite(5, TimeUnit.SECONDS) // 写入后自动刷新时间
                        .build(key -> loadFromRedis());
                addressSet = memberAddressCache.get(SOLANA.getChainShortName());
            }
        } catch (Exception e) {
            log.error("matchLocalAddress error:{}", e.getMessage());
        }
        return CollUtil.isNotEmpty(addressSet) && addressSet.contains(address.toLowerCase());
    }


    private Set<String> loadFromRedis() {
        log.info("loadFromRedis");
        Set<String> setsSOL = setOperations.members(SOLANA.getAddressRedisKey() + master);
        HashSet<String> setAll = CollUtil.newHashSet();
        if (CollUtil.isNotEmpty(setsSOL)) {
            setAll.addAll(setsSOL);
        }
        log.info("loadFromRedis SOL size,{}", setAll.size());
        return CollUtil.isNotEmpty(setAll) ? setAll.stream().map(String::toLowerCase).collect(Collectors.toSet()) : null;
    }


    /**
     * 设置接收货币缓存
     *
     * @param toAddress 接收地址
     */
    private void setCacheByReceiveCurrency(String toAddress, String contractAddress) {
        if (StrUtil.isBlank(toAddress) || StrUtil.isBlank(contractAddress)) {
            return;
        }
        try {
            executorService.execute(() -> {
                try {
                    if (matchLocalAddress(toAddress)) {
                        String redisKey = InfrastructureConstants.RedisKey.USER_RECEIVE_CURRENCY + SOLANA.getChainShortName() + ":" + toAddress + ":" + contractAddress;
                        valueOperations.set(redisKey, contractAddress);
                        valueOperations.getOperations().expire(redisKey, 90, TimeUnit.DAYS);
                    }
                } catch (Exception e) {
                    log.error("设置接收货币缓存失败：{}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("设置接收货币缓存---------线程池异常：{}", e.getMessage());
        }
    }


}



