package com.byb.wallet.sol.remote;

import akka.actor.*;
import cn.hutool.core.lang.hash.Hash;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.byb.wallet.common.core.page.AbstractPageQuery;
import com.byb.wallet.sol.ChatClient;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import scala.sys.Prop;

import javax.annotation.Resource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AkkaManagerActorsListener implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(AkkaManagerActorsListener.class);

    public static class Data  {
        public String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getAge() {
            return age;
        }

        public void setAge(long age) {
            this.age = age;
        }

        public long age;
        public Data() {
            name = "data"+System.currentTimeMillis()%100000;
            age = System.currentTimeMillis()%100000;
        }
    }
    // 定义一个token的 Actor
    public class RemoteActor extends AbstractActor {
        ValueOperations<String, String> valueOperations;
        ListOperations<String, String> listOps;

        Bar bar;
        Map<BarType, Bar> barMap = new HashMap<>();
        Trade trade;


        RemoteActor(ListOperations<String, String> listOps) {
            this.listOps = listOps;

//            this.listOps.rightPush()
        }

        @Override
        public void preStart() {

            this.bar = new Bar();
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ChatClient.class, msg -> {
//                        System.out.println(this.getSelf().path().address());
//                        valueOperations.set("SOLANA:"+msg.getName(),msg.getMessage());
                        long startTime = System.currentTimeMillis();
//                        listOps.rightPush("SOLANA:"+msg.getName(), msg.getMessage());
                        // 更新bar,保存到map
                        JSONObject tradeJson = new JSONObject(msg.getMessage());
                        long timeStamp = tradeJson.getLong("timeStamp", 0L);

                        BigDecimal tokenChange =  tradeJson.getBigDecimal("tokenChange");

                        // 通过timeStamp判断，是否要生成新的bar,这个方法放到bar类中
                        // bar中数据，按照时间周期进行保存和生成，判断是相同时间段的进行更新操作
                        // 通过timeStamp来生成barId,不同barType的barId也不一致
                        // 通过id来判断是否保存过redis，如果保存过进行id生成
                        for(BarType barType : BarType.values()) {
                            long barId = barType.getId(timeStamp);
                            Bar bar = barMap.get(barType);
                            if(bar == null) {
                                bar = new Bar();
                                bar.setOpen(tokenChange);
                                bar.setHigh(tokenChange);
                                bar.setLow(tokenChange);
                                bar.setClose(tokenChange);
                                bar.setBarType(barType);
                                bar.setBarId(barId);
                                barMap.put(barType, bar);

                            }else{
                                // 通过timeStamp和barType得到barId，判断是否需要重新生成bar
                                if(barId < bar.getBarId()) return ;
                                if(barId > bar.getBarId()) {
                                    Bar nextBar = new Bar();
                                    nextBar.setBarId(barId);
                                    nextBar.setOpen(tokenChange);
                                    nextBar.setHigh(tokenChange);
                                    nextBar.setLow(tokenChange);
                                    nextBar.setClose(tokenChange);
                                    nextBar.setBarType(barType);
                                    barMap.put(barType, nextBar);
                                } else {
                                    bar.setBarId(barType.getId(timeStamp));
                                    bar.setClose(tokenChange);
                                    barMap.put(barType, bar);
                                }
                            }
//                            bar.setBarId(timeStamp);
//                            bar.setBarType(barType);
                        }

                        // bars入库redis,判断bar如果是首次保存则保存，否则进行更新
                        for(Bar barOne : barMap.values()) {
                            if(barOne.isSaved() == true) {
//                                System.out.println("SOLANA:"+msg.getName()+":"+barOne.getBarType().name());
//                                System.out.println(barOne.isSaved());
//                                System.out.println(barOne.toString());
                                //更新redis中的bar
                                listOps.set("SOLANA:"+msg.getName()+":"+barOne.getBarType().name(),0, JSONUtil.toJsonStr(barOne));
                            } else{
                                listOps.leftPush("SOLANA:"+msg.getName()+":"+barOne.getBarType().name(), JSONUtil.toJsonStr(barOne));
                                listOps.trim("SOLANA:"+msg.getName()+":"+barOne.getBarType().name(), 0, 100);
                                barOne.setSaved(true);
                            }
                        }

                        // 直接保存message，作为trade，入库redis，最多保存10条
                        listOps.leftPush("SOLANA:"+msg.getName()+":"+"TRADE", msg.getMessage());
                        listOps.trim("SOLANA:"+msg.getName()+":"+"TRADE", 0, 100);
//                        if(length > 10)
//                            listOps.trim("SOLANA:"+msg.getName()+"MIN1", 8, 0);
                        System.out.println("Received message from Node B: "
                                + msg.getMessage()
                                + this.toString() +"||"
                                + msg.getName()+"&"
                                + msg.getMessage());
                        System.out.println("处理时间："+(System.currentTimeMillis() - startTime));
                        log.info("redisSaveTime:"+(System.currentTimeMillis() - startTime));

                    })
                    .build();
        }
    }
    // 定义一个actor管理者
    public class ActorManager extends AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder().match(ChatClient.class, msg->{
//                System.out.println(msg.toString());
//                System.out.println("msg===>"+msg+"getName===>"+msg.getName());
                if(msg.getName() == null) return;
                if(starups.containsKey(msg.getName())) {
                    getActor(msg.getName()).tell(msg, self());
                }else {
                    // 添加限制actor数量的逻辑，如果超出某个数值，不进行创建
                    if(starups.size() > 5000) {
                        return ;
                    }
                    putActor(msg.getName(), startActor(this.getContext().getSystem(), msg.getName()));
                    starups.put(msg.getName(),msg.getName());
//                    Thread.sleep(1000);
                    getActor(msg.getName()).tell(msg, self());
                }
            }).build();
        }
    }

    public ActorRef startActor(ActorSystem system, String token) {
        ActorRef remoteActor = system.actorOf(Props.create(RemoteActor.class, () -> new RemoteActor(listOps)), token);
        return remoteActor;
    }

    public static ActorRef getActor(String token) {
        return tokenActors.get(token);
    }

    public static void putActor(String token, ActorRef actorRef) {
        tokenActors.put(token, actorRef);
    }

    public static Map<String, String> starups = new ConcurrentHashMap<>();
    public static Map<String, ActorRef> tokenActors = new HashMap<String, ActorRef>();


    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;
    @Resource(name = "redisTemplate")
    private SetOperations<String, String> setOperations;
    private LoadingCache<String, Set<String>> memberAddressCache;
    @Resource(name = "redisTemplate")
    private HashOperations<String, String, String> hashOperations;
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 自定义配置字符串
        String customConfigStr = "akka {\n" +
                "  actor {\n" +
                "allow-java-serialization = on  \n"+
                "warn-about-java-serializer-usage = on \n"+

        "    provider = \"akka.remote.RemoteActorRefProvider\"\n" +
                "   serialization {"+
                "       serializers {"+
                "           \"jackson-json\" = \"akka.serialization.jackson.JacksonSerializer\""+
                "       }\n"+
                "       serialization-bindings {"+
                "       \"com.byb.wallet.sol.ChatClient\" = \"jackson-json\""+
                "       }"+
                "   }"+
                "   }\n"+
                "  remote {\n" +
                "    artery {\n" +
                "      transport = tcp\n" +
                "      canonical.hostname = \"127.0.0.1\"\n" +
                "      canonical.port = 25525\n" +
                "    }\n" +
                "  }\n" +
//                    "}"+
                "}";

        // 使用 ConfigFactory.parseString 解析配置字符串
        Config customConfig = ConfigFactory.parseString(customConfigStr);

        // 将自定义配置与默认的 application.conf 合并
        Config finalConfig = ConfigFactory.load(customConfig);

        // 创建 ActorSystem
        ActorSystem system = ActorSystem.create("MyActorSystem2", finalConfig);

        // 输出 ActorSystem 的配置信息
        System.out.println("ActorSystem created with custom configuration.");
        System.out.println("Akka log level: " + finalConfig.getString("akka.loglevel"));


        // 启动线程，保存所有启动的actor
        new Thread(
                ()-> {
                    while (true) {
                        System.out.println("actor的数量："+starups.size());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();

//        启动actor管理者
        system.actorOf(Props.create(ActorManager.class, () -> new ActorManager()),"actorManager");
        System.out.println("Node AkkaManagerActorsListener is running...");
    }
}
