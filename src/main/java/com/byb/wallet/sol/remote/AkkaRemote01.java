package com.byb.wallet.sol.remote;

import akka.actor.ActorSelection;
import cn.hutool.core.codec.Base58;
import cn.hutool.json.JSONObject;
import com.byb.wallet.sol.ChatClient;
import com.typesafe.config.ConfigFactory;
import akka.actor.ActorSystem;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
//import org.bitcoinj.base.Base58;

import java.io.Serializable;
import java.time.Duration;
import java.util.Base64;

public class AkkaRemote01 {
    public static class Data implements Serializable{
        public String name;
        public long age;
        Data() {
            name = "data"+System.currentTimeMillis()%100000;
            age = System.currentTimeMillis()%100000;
        }
    }

    public static ActorSelection startAkkaSystem() {
        long startTime = System.currentTimeMillis();
        String base64String = "aV28KVn8s4YQ87hRU2/7Xj/IVI5eqF4P7fb93hkNmhmISz5ozIolo301r1M+M31TcL5qmYDZZVKpxND2mjljCw==";
        System.out.println(Base58.encode(Base64.getDecoder().decode(base64String)));
        System.out.println(System.currentTimeMillis() - startTime);
//        System.out.println(new String(Base64.getDecoder().decode(base64String), "UTF-8"));
        // 自定义配置字符串
        String customConfigStr = "akka {\n" +
                "  actor {\n" +
                "    provider = \"akka.remote.RemoteActorRefProvider\"\n" +
//                "  }\n" +
                "allow-java-serialization = on  \n"+
                "warn-about-java-serializer-usage = off \n"+

                "   serialization {"+
                "       serializers {"+
                "           jackson-json = \"akka.serialization.jackson.JacksonSerializer\""+
                "       }\n"+
                "       serialization-bindings {"+
                "       \"com.byb.wallet.sol.ChatClient\" = jackson-json"+
                "       }\n"+
                "   }"+
                "   }\n"+
                "  remote {\n" +
                "    artery {\n" +
                "      transport = tcp\n" +
                "      canonical.hostname = \"127.0.0.1\"\n" +
                "      canonical.port = 25527\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // 启动actor管理dispatcher

        // 使用 ConfigFactory.parseString 解析配置字符串
        Config customConfig = ConfigFactory.parseString(customConfigStr);

        // 将自定义配置与默认的 application.conf 合并
        Config finalConfig = ConfigFactory.load(customConfig);

        // 创建 ActorSystem
        ActorSystem system = ActorSystem.create("MyActorSystem", finalConfig);

        // 输出 ActorSystem 的配置信息
        System.out.println("ActorSystem created with custom configuration.");
        System.out.println("Akka log level: " + finalConfig.getString("akka.loglevel"));

        // 获取远程 Actor 的引用
        // 发送消息到远程 Actor

        return system.actorSelection("akka://MyActorSystem2@127.0.0.1:25525/user/actorManager");
    }
    public static void main(String[] args) throws Exception{

        ActorSelection actorSelection = startAkkaSystem();
        actorSelection.tell(new ChatClient("abc", new JSONObject("{\"abcRemoteActorFFFF\":12}").toString()), null);
        System.out.println("Message sent to Node AkkaSystem.");

        // 等待一段时间以确保消息发送完成
        try {
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭 ActorSystem
//        system.terminate();
    }
}
