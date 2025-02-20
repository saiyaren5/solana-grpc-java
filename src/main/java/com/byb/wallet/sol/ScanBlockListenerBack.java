//package com.byb.wallet.sol;
//
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.json.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.ApplicationListener;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// * Solana 扫描区块监听
// *
// * @author Byb Team
// * @since 2024-04-19  14:47
// */
//@Component
//public class ScanBlockListenerBack implements ApplicationListener<ContextRefreshedEvent> {
//
//    private static final Logger log = LoggerFactory.getLogger(ScanBlockListenerBack.class);
//    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
//    private static final ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE * 6, CORE_POOL_SIZE * 20, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
//    private static final ExecutorService executorServiceConsumer = new ThreadPoolExecutor(CORE_POOL_SIZE * 6, CORE_POOL_SIZE * 20, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
////    @Resource
////    private AnalysisBlock analysisBlock;
//    @Resource
//    private RpcClient rpcClient;
//
//
//    @Override
//    public void onApplicationEvent(ContextRefreshedEvent event) {
//
//        // 启动grpc
//        new Thread(()->{
////            new StreamClient().grpcChannelStartUp(log);
//
//        }).start();
//
//
//        // testFlag 暂时rpc方式获取数据
//        boolean testFlag = true;
//        if (testFlag) {
//            System.out.println("SolanaScanBlock---" + Thread.currentThread().getName());
//            return;
//        }
//        long lastSlot = 0;
//        do {
//            try {
//                lastSlot = rpcClient.getSlot();
//            } catch (Exception e) {
//                log.error("获取最新Slot异常:", e);
//            }
//        } while (lastSlot <= 0);
//
//        AtomicLong slotRunning = new AtomicLong(lastSlot);
//        BlockingQueue<Map.Entry<Long, JSONObject>> blockingQueue = new LinkedBlockingQueue<>();
//        LinkedBlockingQueue<Long> compensationBlockQueue = new LinkedBlockingQueue<>();
//        LinkedBlockingQueue<Long> compensationBlockQueueTimeOut = new LinkedBlockingQueue<>();
//
//        List<Long> list = new ArrayList<>();
//        List<Long> listGap = new ArrayList<>();
//        new Thread(() -> {
//            while (true) {
//                try {
//                    int size = compensationBlockQueue.size();
//                    if (size > 0) {
//                        Thread.sleep(300L * size);
//                    }
//                    long runSlot = slotRunning.get();
//                    for (int i = 0; i < 2; i++) {
//                        list.add(runSlot + i);
//                    }
//                    list.parallelStream().forEach(slot -> getBlock(slot, compensationBlockQueueTimeOut, compensationBlockQueue, blockingQueue, "正常>>>>>>>>>>>>>>>>>>>>>>>>>>"));
//                    slotRunning.set(runSlot + 2);
//                    list.clear();
//
//
//                    if (runSlot % 4 != 0) {
//                        Long slotBest = rpcClient.getSlot();
//                        if (slotBest > 0) {
//                            long l = slotRunning.get();
//                            long gap = slotBest - l;
//                            if (gap > 5) {
//                                log.info("与最新slot差距：{}", gap);
//                                for (int i = 0; i < gap; i++) {
//                                    listGap.add(l + i);
//                                }
//                                listGap.forEach(slot -> {
//                                    try {
//                                        executorService.execute(() -> getBlock(slot, compensationBlockQueueTimeOut, compensationBlockQueue, blockingQueue, "差距补偿-------------------------------"));
//                                    } catch (Exception e) {
//                                        log.error("差距补偿-----线程池异常: {}", e.getMessage());
//
//                                    }
//                                });
//
//                                slotRunning.set(l + gap);
//                                listGap.clear();
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("获取区块任务异常: ", e);
//                }
//            }
//        }).start();
//
//        new Thread(() -> {
//            while (true) {
//                try {
//                    if (CollUtil.isNotEmpty(compensationBlockQueue)) {
//                        while (true) {
//                            Long slotCompensation = compensationBlockQueue.poll();
//                            if (slotCompensation == null || slotCompensation < 0) {
//                                break;
//                            }
//                            try {
//                                executorService.execute(() -> getBlock(slotCompensation, compensationBlockQueueTimeOut, compensationBlockQueue, blockingQueue, "失败补偿xxxxxxxxxxxxxxxxxxxxxxxxxxxx"));
//                            } catch (Exception e) {
//                                log.error("补偿获取区块-----线程池异常: {}", e.getMessage());
//                            }
//                        }
//                    }
//                    if (CollUtil.isNotEmpty(compensationBlockQueueTimeOut)) {
//                        while (true) {
//                            Long slotCompensation = compensationBlockQueueTimeOut.poll();
//                            if (slotCompensation == null || slotCompensation < 0) {
//                                break;
//                            }
//                            try {
//
//                                executorService.execute(() -> getBlock(slotCompensation, compensationBlockQueueTimeOut, compensationBlockQueue, blockingQueue, "超速补偿+++++++++++++++++++++++++++++"));
//                            } catch (Exception e) {
//                                log.error("获取超速区块-----线程池异常: {}", e.getMessage());
//                            }
//                        }
//                    }
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    log.error("补偿获取区块任务异常: ", e);
//                }
//
//            }
//        }).start();
//
//
//        Runnable solAnalysisTask = () -> {
//            Map.Entry<Long, JSONObject> entryMap = blockingQueue.poll();
//            if (entryMap == null) {
//                return;
//            }
//            try {
//                // 从阻塞队列中取出任务
//                Long slot = entryMap.getKey();
//                log.info("消费解析Slot: " + slot);
//                executorServiceConsumer.execute(() -> {
//                    try {
//                        long startTime = System.currentTimeMillis();
////                        analysisBlock.parseSave(slot, entryMap.getValue());
//                        log.info("解析时间: {} 秒", (System.currentTimeMillis() - startTime) / 1000);
//
//                    } catch (Exception e) {
//                        blockingQueue.add(entryMap);
//                        log.info("解析区块异常: ", e);
//                    }
//                });
//            } catch (Exception e) {
//                log.error("解析区块-------线程池异常: {}", e.getMessage());
//            }
//        };
//
//
//        new Thread(() -> {
//            while (true) {
//                try {
//                    solAnalysisTask.run();
//                    Thread.sleep(50);
//                } catch (InterruptedException ignored) {
//                }
//            }
//        }).start();
//
//    }
//
//    private void getBlock(Long slot, LinkedBlockingQueue<Long> compensationBlockQueueTimeOut, LinkedBlockingQueue<Long> compensationBlockQueue, BlockingQueue<Map.Entry<Long, JSONObject>> blockingQueue, String source) {
//        try {
//
//            long startTime = System.currentTimeMillis();
//            JSONObject result = null;
//            try {
//                result = rpcClient.getBlockBySlot(slot);
//            } catch (Exception e) {
//                log.error(source + "获取区块Error: ", e);
//            }
//
//            if (result != null) {
//
//                if (result.containsKey("error")) {
//                    if (result.toString().contains("-32004")) {
//                        compensationBlockQueue.add(slot);
//                        log.info(source + "{{{{{{{-32004}}}}}}}}}}----------Slot: {}", slot);
//                        log.info("补偿区块容量大小: {}", compensationBlockQueue.size());
//                    } else {
//                        log.info(source + "获取区块错误，Slot：{}   错误码：{}", slot, result.getJSONObject("error").getLong("code"));
//                    }
//                } else {
//                    JSONObject block = result.getJSONObject("result");
//                    if (block == null || block.isEmpty() || !block.containsKey("transactions") || CollUtil.isEmpty(block.getJSONArray("transactions"))) {
//                        log.info(source + "获取区块的交易数据为空，Slot: {}, block:{}", slot, block);
//                        return;
//                    }
//                    log.info(source + "区块队列大小：{}", blockingQueue.size());
//                    log.info(source + "生产区块Slot:{} 时间:{} 秒", slot, (double) (System.currentTimeMillis() - startTime) / 1000);
//                    blockingQueue.add(new AbstractMap.SimpleEntry<>(slot, block));
//                }
//            } else {
//                log.info(source + "获取区块为空，Slot: {}", slot);
//                compensationBlockQueueTimeOut.add(slot);
//                log.info("超速区块容量大小: {}", compensationBlockQueueTimeOut.size());
//            }
//        } catch (Exception e) {
//            log.error("获取区块-getBlock----异常: ", e);
//        }
//    }
//
//}
