package com.byb.wallet.sol;

import cn.hutool.core.codec.Base58;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import geyser.GeyserGrpc;
import geyser.GeyserOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

//import example.ServiceOuterClass.*;
//import example.StreamServiceGrpc;

public class StreamClientSwapWSOL {
    public static void main(String[] args) {

        StreamObserver<GeyserOuterClass.SubscribeRequest> requestObserver = null;
//        ManagedChannel channel = ManagedChannelBuilder.forAddress("go.getblock.io", 443)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("grpc.fra.shyft.to", 443)
//                .usePlaintext()
                // 默认是4m，此处配置为10M
                .maxInboundMessageSize(10 * 1024 * 1024) // 设置最大接收消息大小为 10MB
//                .maxOutboundMessageSize(10 * 1024 * 1024) // 设置最大发送消息大小为 10MB

                .build();

        GeyserGrpc.GeyserStub stub = GeyserGrpc.newStub(channel);
        // 创建 Metadata 并添加 x-token
//        Metadata metadata = new Metadata();
//        Metadata.Key<String> authKey = Metadata.Key.of("x-access-token", Metadata.ASCII_STRING_MARSHALLER);
//        metadata.put(authKey, "valid-token");
//        stub = MetadataUtils.newAttachHeadersInterceptor(stub, metadata);


        // 创建Metadata并设置x-token头
        Metadata metadata = new Metadata();
        Metadata.Key<String> xTokenKey = Metadata.Key.of("x-token", Metadata.ASCII_STRING_MARSHALLER);
//        metadata.put(xTokenKey, "ad2809e97d1340c0a1bb946982884461");
        metadata.put(xTokenKey, "07f4c017-6a3e-4cc0-9759-9ccdcc37489c");

        // 使用MetadataUtils工具创建拦截器
        io.grpc.ClientInterceptor headersInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);

        // 将拦截器附加到存根
        stub = stub.withInterceptors(headersInterceptor);


        requestObserver = stub.subscribe(new StreamObserver<GeyserOuterClass.SubscribeUpdate>() {
            @Override
            public void onNext(GeyserOuterClass.SubscribeUpdate transaction) {
//                System.out.println(response.getFiltersList());

                System.out.println("transaction is :::::");
                System.out.println(transaction.getTransaction());


//                solana.storage.ConfirmedBlock.SolanaStorage.TransactionStatusMeta metaData = transaction.getTransaction().getTransaction().getMeta();


//                transaction.getTransaction().getTransaction().getTransaction().getMessage().getAccountKeysList().forEach(o->{
////                    System.out.println(o);
//
////                    o.toByteArray()
//
//                    byte[] bytes = o.toByteArray();
//                    System.out.println("https://solscan.io/tx/"+Base58.encode(bytes));
//                });

//                log.info("getSLot::::"+transaction.getTransaction().getSlot());



//                JSONObject transaction = tx.getJSONObject("transaction");
//                JSONArray accountKeys = transaction.getJSONArray("accountKeys");
//                String txHash = transaction.getJSONArray("signatures").get(0).toString();
//                JSONObject txMeta = tx.getJSONObject("meta");





//                System.out.println("response.getPing::::"+response.getPing().toString());
//                System.out.println("response.getPong::::"+response.getPong().toString());
////                System.out.println("结果输出："+response+"结果输出结束");
//                System.out.println("getSlot:::"+response.getBlock().getSlot());
////                System.out.println("response::"+response.getBlock());
//                response.getBlock().getTransactionsList().forEach(o->{
//                    if(1==1) return;
////                    byte[] bytes = transaction.getSignature().toByteArray();
////                    System.out.println("https://solscan.io/tx/"+Base58.encode(bytes));
//                    byte[] bytes = o.getSignature().toByteArray();
//                System.out.println("Received: " + Base58.encode(bytes)        );
////                    System.out.println("getSignature===>"+o.getSignature().toStringUtf8());
//                });

//                if(response.toString().contains("ping")&& 1==2) {
//                    response.getEntry();
//                    System.out.println("获取到server端的ping"+response.toString());
//                    GeyserOuterClass.SubscribeRequest.Builder subscribeRequestBuilder = GeyserOuterClass.SubscribeRequest.newBuilder();
////                    requestObserver.onNext(null);
//                    subscribeRequestBuilder.setPing(GeyserOuterClass.SubscribeRequestPing.newBuilder().build());
////                    this.onCompleted();
//
////                    requestObserver.onNext(subscribeRequestBuilder.build());
//
//                }
//
////                response.
//                byte[] bytes = response.getTransaction().toByteArray();
//                System.out.println("Received: " + Base58.encode(bytes)        );
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
//                            .getAllFields()
//                            .set
                            .addAccountInclude("So11111111111111111111111111111111111111112")
//                            .addAccountInclude("11111111111111111111111111111111")
//                            .addAccountInclude("Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB")
//                            .addAccountInclude("*")
                    .setIncludeEntries(false).setIncludeTransactions(true).setIncludeAccounts(false).build());
            Map<String, GeyserOuterClass.SubscribeRequestFilterTransactions> transactionsMap = new HashMap<>();
//            transactionsMap.put("transaction1", GeyserOuterClass.SubscribeRequestFilterTransactions.newBuilder().setVote(false).build());
            transactionsMap.put("transaction2",
                    GeyserOuterClass.SubscribeRequestFilterTransactions.newBuilder()
                            .setFailed(true).setVote(false)
                    .build());
            subscribeRequestBuilder.putAllTransactions(transactionsMap);

//            subscribeRequestBuilder.setPing()
            System.out.println("获取pingRequest:"+subscribeRequestBuilder.getPing());
//            subscribeRequestBuilder.setPing(GeyserOuterClass.SubscribeRequestPing.newBuilder().build());



//            System.out.println(blocksMap.values());
            // 屏蔽blocks数据
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
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.shutdown();
    }
}