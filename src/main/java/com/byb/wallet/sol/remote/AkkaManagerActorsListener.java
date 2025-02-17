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
                        // 更新bar
                        BigDecimal tokenChange =  new JSONObject(msg.getMessage()).getBigDecimal("tokenChange");
                        bar.setClose(tokenChange);
                        long length = listOps.rightPush("SOLANA:"+msg.getName()+"MIN1", JSONUtil.toJsonStr(bar));

                        // 生成barMap，
                        for(BarType barType : BarType.values()) {
                            if(barMap.get(barType) != null) {
                                barMap.get(barType).setClose(tokenChange);
                                barMap.put(barType, barMap.get(barType));
                            } else {
                                Bar newBar = new Bar();
                                newBar.setBarType(barType);
                                barMap.put(barType, newBar);
                            }
                        }
                        // bars入库redis
                        for(Bar barOne : barMap.values()) {
                            listOps.rightPush("SOLANA:"+msg.getName()+":"+barOne.getBarType().name(), JSONUtil.toJsonStr(barOne));

                        }
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
                if(starups.containsKey(msg.getName())) {
                    getActor(msg.getName()).tell(msg, self());
                }else {
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
