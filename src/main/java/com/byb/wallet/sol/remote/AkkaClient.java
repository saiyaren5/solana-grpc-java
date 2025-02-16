package com.byb.wallet.sol.remote;

import akka.actor.ActorSelection;
import cn.hutool.core.codec.Base58;
import cn.hutool.json.JSONObject;
import com.byb.wallet.sol.ChatClient;
import geyser.GeyserGrpc;
import geyser.GeyserOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import solana.storage.ConfirmedBlock.SolanaStorage;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.byb.wallet.sol.remote.AkkaRemote01.startAkkaSystem;

//import example.ServiceOuterClass.*;
//import example.StreamServiceGrpc;

public class AkkaClient {
    public static void main(String[] args) {

        StreamObserver<GeyserOuterClass.SubscribeRequest> requestObserver = null;
        ManagedChannel channel = ManagedChannelBuilder.forAddress("grpc.fra.shyft.to", 443)
//                .usePlaintext()
                .keepAliveTime(10, TimeUnit.SECONDS)
                .build();
        GeyserGrpc.GeyserStub stubSolana = GeyserGrpc.newStub(channel);


        // 创建Metadata并设置x-token头
        Metadata metadata = new Metadata();
        Metadata.Key<String> xTokenKey = Metadata.Key.of("x-token", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(xTokenKey, "07f4c017-6a3e-4cc0-9759-9ccdcc37489c");

        // 使用MetadataUtils工具创建拦截器
        io.grpc.ClientInterceptor headersInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);

        // 将拦截器附加到存根
        stubSolana = stubSolana.withInterceptors(headersInterceptor);

        ActorSelection actorSelection = startAkkaSystem();


        requestObserver = stubSolana.subscribe(new StreamObserver<GeyserOuterClass.SubscribeUpdate>() {
            @Override
            public void onNext(GeyserOuterClass.SubscribeUpdate response) {

                if(1==1) {
                    if((response.toString().contains("ping") || response.toString().contains("pong")) && response.toString().length()< 1000) {
                        System.out.println(response.toString());
                        return;
                    }

                    // 对solana-transaction记录的结构解析：
                    // 1.message中获取accountKeys和instructions
                    // 2.meta中获取fee、"postBalances"、"innerInstructions"、"postTokenBalances"

                    // 解析流程
                    // 1.获取accountKeys,匹配sol交易及spl-token交易记录相关
                    // token program 和 associate token account program记录index及address
                    // 可以扩展解析更多program类型，如pump.fun,raydium,orca,okx等
                    // 2.解析instructions，包装sol及tokens交易记录
                    // 3.解析innerInstructions，包装sol及tokens交易记录
                    // 4.上述解析token交易时，涉及到的mint需要通过accountKeys定位查询
                    // 目前主要查找spl-token和sol相关交易记录，进行保存

                    // 过滤流程
                    // 1.筛选出sol和spl-token交易记录
                    // 2.匹配from和to地址是否在内存地址中
                    // 3.包装交易记录


                    byte[] bytes = response.getTransaction().getTransaction().getSignature().toByteArray();
//                    System.out.println("getSignature: " + Base58.encode(bytes));
                    //遍历instructions和innerInstructions
                    SolanaStorage.TransactionStatusMeta metaInfo =
                            response.getTransaction().getTransaction().getMeta();

//                    metaInfo.getPreTokenBalancesList().stream().filter(o->o.getAccountIndex() == 1);
//                    metaInfo.getFee();
//                    metaInfo.getInnerInstructionsList();
//                    metaInfo.getPostBalancesList();

                    JSONObject trade = new JSONObject();
                    List<solana.storage.ConfirmedBlock.SolanaStorage.TokenBalance> tokenBalances = metaInfo.getPreTokenBalancesList();

//                    System.out.println("sizePreBalance==>"+tokenBalances.size());
//                    System.out.println("sizePostBalance==>"+metaInfo.getPostTokenBalancesList().size());
//                    System.out.println(tokenBalances.toString());
//                    System.out.println(metaInfo.getPostTokenBalancesList().toString());
                    for(int i=0; i<tokenBalances.size(); i++){
                        if(!tokenBalances.get(i).getOwner().equals("5Q544fKrFoe6tsEbD7S8EmxGTJYAKtTVhAW5Q5pge4j1"))
                            continue;
                        int accountIndex = tokenBalances.get(i).getAccountIndex();
                        trade.set("signature", Base58.encode(bytes));
                        //判断是WSOL还是TOKEN
                        if(tokenBalances.get(i).getMint().equals("So11111111111111111111111111111111111111112")){

                            BigDecimal preTokenBalance = new BigDecimal(tokenBalances.get(i).getUiTokenAmount().getUiAmountString());
                            BigDecimal postTokenBalance = new BigDecimal(metaInfo.getPostTokenBalancesList().stream().filter(o->o.getAccountIndex()==accountIndex).findFirst().get().getUiTokenAmount().getUiAmountString());

                            if(preTokenBalance.compareTo(postTokenBalance) == 0) continue;
                            if(preTokenBalance.compareTo(postTokenBalance) < 0){
                                trade.set("type", 0);
                            }else{
                                trade.set("type", 1);
                            }
                            trade.set("wSOLChange", postTokenBalance.subtract(preTokenBalance));
                        }else{
                            BigDecimal postTokenBalance = new BigDecimal(metaInfo.getPostTokenBalancesList().stream().filter(o->o.getAccountIndex()==accountIndex).findFirst().get().getUiTokenAmount().getUiAmountString());
                            BigDecimal preTokenBalance = new BigDecimal(tokenBalances.get(i).getUiTokenAmount().getUiAmountString());
                            trade.set("token", tokenBalances.get(i).getMint());
                            if(preTokenBalance.compareTo(postTokenBalance) == 0) continue;
                            trade.set("tokenChange", postTokenBalance.subtract(preTokenBalance));
                        }
                    }
//                    将trade信息推送给actor
                    System.out.println("trade===>"+trade);
//                    System.out.println("getTransaction===>"+response.getTransaction().getSlot());

                    if(trade.getStr("token") != null )
                        actorSelection.tell(new ChatClient(trade.getStr("token"), new JSONObject(trade).toString()), null);

                    return;
                }
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
            Map<String, GeyserOuterClass.SubscribeRequestFilterBlocksMeta> blocksMetaHashMap = new HashMap<>();

            blocksMetaHashMap.put("blockMeta",GeyserOuterClass.SubscribeRequestFilterBlocksMeta.newBuilder()
                    .build());
            Map<String, GeyserOuterClass.SubscribeRequestFilterTransactions> transactionsRaydiumMap = new HashMap<>();
//            transactionsMap.put("transaction1", GeyserOuterClass.SubscribeRequestFilterTransactions.newBuilder().setVote(false).build());
            transactionsRaydiumMap.put("transactionRaydium",
                    GeyserOuterClass.SubscribeRequestFilterTransactions.newBuilder()
                            //添加订阅pump功能
                            .addAccountInclude("6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P")
                            .addAccountInclude("675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8")
                            .setFailed(false).setVote(false)
                            .build());
//            取消订阅transactions
            subscribeRequestBuilder.putAllTransactions(transactionsRaydiumMap);
            requestObserver.onNext(subscribeRequestBuilder.build());

            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscribeRequestBuilder.setPing(GeyserOuterClass.SubscribeRequestPing.newBuilder().build());
                requestObserver.onNext(subscribeRequestBuilder.build());
            }
        }
        // Close the stream
        requestObserver.onCompleted();

        // Shutdown the channel
        try {
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.shutdown();
    }
}