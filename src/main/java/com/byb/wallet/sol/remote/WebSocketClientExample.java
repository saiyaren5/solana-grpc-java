package com.byb.wallet.sol.remote;

import akka.Done;
import akka.actor.ActorSystem;
//import akka.actor.typed.ActorSystem;
//import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.concurrent.CompletionStage;

public class WebSocketClientExample {
    public static void main(String[] args) {
        // 创建 ActorSystem
        ActorSystem system = ActorSystem.create("WebSocketClient");

        // 定义 WebSocket 连接的 URI
        String uri = "ws://echo.websocket.org";
        WebSocketRequest request = WebSocketRequest.create(uri);

        // 定义发送消息的 Source
        Source<Message,?> outgoing = Source.single(TextMessage.create("Hello, WebSocket Server!"));

        // 定义接收消息的 Sink
        Sink<Message, CompletionStage<Done>> incoming = Sink.foreach(message -> {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                System.out.println("Received message: " + textMessage.getStrictText());
            } else {
                System.out.println("Received non-text message");
            }
        });

        // 创建 WebSocket 流
        Flow<Message, Message, CompletionStage<WebSocketUpgradeResponse>> flow = Http.get(system).webSocketClientFlow(request);

        // 连接 Source 和 Sink 到 WebSocket 流
//        CompletionStage<WebSocketUpgradeResponse> upgradeResponse =
//                outgoing.viaMat(flow, java.util.function.Function.identity())
//                        .toMat(incoming, java.util.function.BiFunction::apply)
//                        .run(system);
//
//        // 检查 WebSocket 连接是否成功升级
//        upgradeResponse.whenComplete((upgrade, ex) -> {
//            if (ex!= null) {
//                System.err.println("Connection failed: " + ex.getMessage());
//            } else if (upgrade.response().status().intValue()!= 101) {
//                System.err.println("Connection failed: " + upgrade.response().status());
//            } else {
//                System.out.println("Connected successfully");
//            }
//            // 关闭 ActorSystem
//            system.terminate();
//        });
    }
}