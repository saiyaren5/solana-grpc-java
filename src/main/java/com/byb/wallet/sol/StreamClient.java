package com.byb.wallet.sol;

import cn.hutool.core.codec.Base58;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.byb.wallet.infrastructure.common.block.sol.entity.TransactionRecordSol;
import geyser.GeyserGrpc;
import geyser.GeyserOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
//import example.ServiceOuterClass.*;
//import example.StreamServiceGrpc;

//import java.geyser.GeyserOuterClass;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class StreamClient {
//    public static void main(String[] args) {
    public static void grpcChannelStartUp(Logger log) {
//        ManagedChannel channel = ManagedChannelBuilder.forAddress("go.getblock.io", 443)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("grpc.fra.shyft.to", 443)
//                .usePlaintext()
                .build();

        GeyserGrpc.GeyserStub stub = GeyserGrpc.newStub(channel);
        // 创建 Metadata 并添加 x-token
//        Metadata metadata = new Metadata();
//        Metadata.Key<String> authKey = Metadata.Key.of("x-access-token", Metadata.ASCII_STRING_MARSHALLER);
//        metadata.put(authKey, "valid-token");
//        stub = MetadataUtils.newAttachHeadersInterceptor(stub, metadata);

        // 创建Metadata并设置x-token头
        Metadata metadata = new Metadata();
//        Metadata.Key<String> xTokenKey = Metadata.Key.of("x-access-token", Metadata.ASCII_STRING_MARSHALLER);
        Metadata.Key<String> xTokenKey = Metadata.Key.of("x-token", Metadata.ASCII_STRING_MARSHALLER);
//        metadata.put(xTokenKey, "ad2809e97d1340c0a1bb946982884461");
        metadata.put(xTokenKey, "07f4c017-6a3e-4cc0-9759-9ccdcc37489c");

        // 使用MetadataUtils工具创建拦截器
        io.grpc.ClientInterceptor headersInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);

        // 将拦截器附加到存根
        stub = stub.withInterceptors(headersInterceptor);


        StreamObserver<GeyserOuterClass.SubscribeRequest> requestObserver = stub.subscribe(new StreamObserver<GeyserOuterClass.SubscribeUpdate>() {
            @Override
            public void onNext(GeyserOuterClass.SubscribeUpdate transaction) {
                transaction.getTransaction().getTransaction().getTransaction().getMessage().getAccountKeysList().forEach(o-> {
////                    System.out.println(o);
//
////                    o.toByteArray()
//
                    byte[] bytes = o.toByteArray();
                    System.out.println("https://solscan.io/tx/"+Base58.encode(bytes));
//                });
                });
                log.info("getSlot:::"+transaction.getTransaction().getSlot());

//                JSONArray instructions = message.getJSONArray("instructions");
                solana.storage.ConfirmedBlock.SolanaStorage.Message message = transaction.getTransaction().getTransaction().getTransaction().getMessage();
                message.getInstructionsList().forEach(instruction->{
                    instruction.getProgramIdIndex();
//                    instruction.g
                    String programId = Base58.encode(message.getAccountKeys(instruction.getProgramIdIndex()).toByteArray());
//                    instruction.getAllFields()
//                            instruction.getParserForType()
                    log.info("instruction:::{},programId:::{}",instruction.toString(), programId);



                });
//                message.getHeader()
//                if (CollUtil.isNotEmpty(instructions)) {
//                    instructions.forEach(instruction -> {
//                        JSONObject instructionJSONObject = JSONUtil.parseObj(instruction);
//                        String programId = instructionJSONObject.getStr("programId");
//                        String program = instructionJSONObject.getStr("program");
                        // 系统程序
//                        if ("11111111111111111111111111111111".equalsIgnoreCase(programId)) {
//                            JSONObject parsed = instructionJSONObject.getJSONObject("parsed");
//                            if (parsed != null && !parsed.isEmpty()) {
//                                String type = parsed.getStr("type");
//                                if (StrUtil.isNotBlank(type)) {
//                                    // SOL转账
//                                    if ("transfer".equalsIgnoreCase(type)) {
//                                        JSONObject parsedInfo = parsed.getJSONObject("info");
//                                        buildSolTransactionData(parsedInfo, transactionSaveMap, slot, blockHeight, blockHash, txHash, finalTransactionTime, fee, computeUnitsConsumed, status, version, signer, program);
//                                        //} else if ("create_account".equalsIgnoreCase(type)) {
//                                        // TODO 示例 后续看是否需要解析
//                                        //  "parsed": {
//                                        //    "info": {
//                                        //      "lamports": 2039280,
//                                        //      "newAccount": "GEQLVxWrqyNFysX2yo3ndw33TsZgixyguDhkGCWwWxF3",
//                                        //      "owner": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
//                                        //      "source": "BfktaV82c44Rb8MjsEwKnupp6AtL8KCM2iUH528jKEuk",
//                                        //      "space": 165
//                                        //    },
//                                        //    "type": "createAccount"
//                                        //  },
//                                        //  "program": "system",
//                                        //  "programId": "11111111111111111111111111111111",
//                                        //  "stackHeight": 2
//                                    }
//                                }
//                            }
//                        }
//
//                        // 在Solana网络中，"programId": "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"代表的是Associated Token Program的ID。
//                        // 这个程序的主要作用是帮助创建并管理与特定Solana钱包相关
//                        else if ("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL".equalsIgnoreCase(programId)
//                                || "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA".equalsIgnoreCase(programId)) {
//                            JSONObject parsed = instructionJSONObject.getJSONObject("parsed");
//                            if (parsed != null && !parsed.isEmpty()) {
//                                String type = parsed.getStr("type");
//                                if (StrUtil.isNotBlank(type)) {
//                                    // 代币转账
//                                    // 在Solana区块链中，"type": "transferChecked"是Token Program的一种指令类型，用于在特定的代币账户之间进行转账操作，并且会执行额外的安全性检查。
//                                    // transferChecked指令相对于普通的transfer指令来说，多了一些验证步骤，以确保代币的精度和数量符合预期。
//                                    if ("transferChecked".equalsIgnoreCase(type) || "transfer".equalsIgnoreCase(type)) {
//                                        JSONObject parsedInfo = parsed.getJSONObject("info");
//                                        String fromTokenAddress = parsedInfo.getStr("source");
//                                        String toTokenAddress = parsedInfo.getStr("destination");
//                                        String contractAddress = parsedInfo.getStr("mint");
//
//                                        HashMap<String, String> fromAddressAndToAddressMap = new HashMap<>(8);
//                                        Map<String, String> contractAddressMap = new HashMap<>(8);
//                                        JSONArray postTokenBalances = meta.getJSONArray("postTokenBalances");
//                                        JSONArray preTokenBalances = meta.getJSONArray("preTokenBalances");
//                                        for (int i = 0; i < accountKeys.size(); i++) {
//                                            if (JSONUtil.parseObj(accountKeys.get(i)).getStr("pubkey").equalsIgnoreCase(fromTokenAddress)) {
//                                                getFromAddrOrToAddr(postTokenBalances, i, contractAddressMap, fromAddressAndToAddressMap, fromTokenAddress, preTokenBalances);
//                                            } else if (JSONUtil.parseObj(accountKeys.get(i)).getStr("pubkey").equalsIgnoreCase(toTokenAddress)) {
//                                                getFromAddrOrToAddr(postTokenBalances, i, contractAddressMap, fromAddressAndToAddressMap, toTokenAddress, preTokenBalances);
//                                            }
//                                        }
//
//                                        if (StrUtil.isBlank(contractAddress)) {
//                                            contractAddress = contractAddressMap.get(fromTokenAddress);
//                                        }
//
//                                        String fromAddress = null;
//                                        if (CollUtil.isEmpty(fromAddressAndToAddressMap) || !fromAddressAndToAddressMap.containsKey(fromTokenAddress)) {
//                                            log.error("创建代币账户并转账，获取from-owner地址为空，交易哈希：{}，fromTokenAddress：{}", txHash, fromTokenAddress);
//                                            fromAddress = fromTokenAddress;
//                                        } else {
//                                            fromAddress = fromAddressAndToAddressMap.get(fromTokenAddress);
//                                        }
//
//                                        String toAddress = null;
//                                        if (CollUtil.isEmpty(fromAddressAndToAddressMap) || !fromAddressAndToAddressMap.containsKey(toTokenAddress)) {
//                                            log.error("创建代币账户并转账，获取from-owner地址为空，交易哈希：{}，toTokenAddress：{}", txHash, toTokenAddress);
//                                            toAddress = toTokenAddress;
//                                        } else {
//                                            toAddress = fromAddressAndToAddressMap.get(toTokenAddress);
//                                        }
//
//                                        //setContractAddressInRedis(contractAddressMap.values().stream().distinct().collect(Collectors.toList()));
//
////                                        if (!matchLocalAddress(fromAddress) && !matchLocalAddress(toAddress)) {
////                                            return;
////                                        }
////                                        setCacheByReceiveCurrency(toAddress, contractAddress);
//                                        String value = "0";
//                                        if ("transfer".equalsIgnoreCase(type)) {
//                                            value = parsedInfo.getStr("amount");
//                                        } else if ("transferChecked".equalsIgnoreCase(type)) {
//                                            value = parsedInfo.getJSONObject("tokenAmount").getStr("amount");
//                                        }
//                                        transactionSaveMap.put(txHash + "-" + fromAddress + "-" + toAddress + "-" + value + "-" + contractAddress, new TransactionRecordSol(txHash, blockHash, slot, blockHeight, computeUnitsConsumed, fee, fromAddress, fromTokenAddress, toAddress, toTokenAddress, value, status, contractAddress, finalTransactionTime, program, 1, signer, version));
//                                    }
//                                    // create
//                                    // closeAccount
//
//                                }
//                            }
//                        }
//                    });

//                }
            }

                public void onNextGetBlock(GeyserOuterClass.SubscribeUpdate response) {
                System.out.println(response.getFiltersList());
//                if(1==1) return;
//                System.out.println(response.getBlock().getTransactions(111));
                System.out.println(response.getBlock().getBlockTime().getTimestamp());
//                System.out.println(new Date(response.getBlock().getBlockTime()));
//
                // 将时间戳转为UTC格式
                long secondsSinceEpoch = response.getBlock().getBlockTime().getTimestamp();
                Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
                // Convert to ZonedDateTime with a specific timezone
                ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
                // Format the date
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz");
                String formattedDate = zonedDateTime.format(formatter);
                log.info("Formatted Date: " + formattedDate);
                log.info("Transaction count:"+response.getBlock().getTransactionsList().size());
                log.info("Block Slot:"+response.getBlock().getSlot());
                if(response.getBlock().getTransactionsList().size() == 0 ) {
                    log.info("Block response:"+response);
                    return ;
                }

                // 暂时注释，观察slot变化
//                response.getBlock().getTransactionsList().forEach(o->{
//                    System.out.println("TransactionSignature::"+Base58.encode(o.getSignature().toByteArray()));
////                    System.out.println(o.getMeta());
//                });

//                if(response.getBlock().getTransactionsList().size()>0)
//                System.out.println(response.getBlock().getTransactionsList().get(0).getSignature());
//                System.out.println(Base58.encode(response.getBlock().getTransactionsList().get(0).getSignature().toByteArray()));


//                System.out.println(response.getEntry());
                byte[] bytes = response.getTransaction().toByteArray();
//                System.out.println("Received: " + Base58.encode(bytes)        );

                System.out.println("hashSignature::"+Base58.encode(response.getTransaction().getTransaction().getSignature().toByteArray()));
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                System.err.println("Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Server completed");
            }
        });

        GeyserOuterClass.SubscribeRequest.Builder subscribeRequestBuilder = GeyserOuterClass.SubscribeRequest.newBuilder();

        // Send some messages
        for (int i = 0; i < 1; i++) {
            String message = "Message " + i;
            Map<String, GeyserOuterClass.SubscribeRequestFilterBlocksMeta> blocksMetaHashMap = new HashMap<>();

            blocksMetaHashMap.put("blockMeta",GeyserOuterClass.SubscribeRequestFilterBlocksMeta.newBuilder()
            .build());

            Map<String, GeyserOuterClass.SubscribeRequestFilterBlocks> blocksMap = new HashMap<>();
            blocksMap.put("block01",
                    GeyserOuterClass.SubscribeRequestFilterBlocks.newBuilder()
//                            .addAccountInclude("So11111111111111111111111111111111111111112")
//                            .addAccountInclude("So11111111111111111111111111111111111111112")
//                            .addAccountInclude("5Q544fKrFoe6tsEbD7S8EmxGTJYAKtTVhAW5Q5pge4j1")
//                            .addAccountInclude("3Y318iUcwLUq8FGiDuTT11nGwhSynMC1nPZdXKNHuDkD")
//                            .addAccountInclude("6m2CDdhRgxpH4WjvdzxAYbGxwdGUz5MziiL5jek2kBma")
//                            .addAccountInclude("Dd12znRTCHMUrQjrELMJ2QYAGo1inNf6rSFwmUohVryy")
                            .addAccountInclude("6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P")
//                            .setField(Descriptors.FieldDescriptor.JavaType,"")
//                            .addAccountInclude("5Q544fKrFoe6tsEbD7S8EmxGTJYAKtTVhAW5Q5pge4j1")
//                            .setAccountInclude(0,"So11111111111111111111111111111111111111112")
                    .setIncludeEntries(false).setIncludeTransactions(true).setIncludeAccounts(false).build());

//            subscribeRequestBuilder.putAllBlocks(blocksMap);

            Map<String, GeyserOuterClass.SubscribeRequestFilterTransactions> transactionsMap = new HashMap<>();
//            transactionsMap.put("transaction1", GeyserOuterClass.SubscribeRequestFilterTransactions.newBuilder().setVote(false).build());
            transactionsMap.put("transaction2",
                    GeyserOuterClass.SubscribeRequestFilterTransactions.newBuilder()
                            .setFailed(false).setVote(false)
                    .build());
            subscribeRequestBuilder.putAllTransactions(transactionsMap);


            Map<String, GeyserOuterClass.SubscribeRequestFilterEntry> entryMap = new HashMap<>();
            entryMap.put("entries",
                    GeyserOuterClass.SubscribeRequestFilterEntry.newBuilder().build()
//                    .set
            );
//            subscribeRequestBuilder.putAllEntry(entryMap);



//            System.out.println(blocksMap.values());
//            subscribeRequestBuilder.putAllBlocks(blocksMap);
//            subscribeRequestBuilder.putAllBlocksMeta(blocksMetaHashMap);
//            subscribeRequestBuilder.putAllBlocks(blocksMap);
//            subscribeRequestBuilder.putBlocks(blocksMap.);

//            subscribeRequestBuilder.map
//            GeyserOuterClass.SubscribeRequest request = GeyserOuterClass.SubscribeRequest.newBuilder()
//                    .setField(Descriptors.FieldDescriptor)
//                    .build();
            requestObserver.onNext(subscribeRequestBuilder.build());
//            requestObserver.onNext(transactionsMap0.build());
        }

        // Close the stream
        requestObserver.onCompleted();

        // Shutdown the channel
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.shutdown();
    }
}