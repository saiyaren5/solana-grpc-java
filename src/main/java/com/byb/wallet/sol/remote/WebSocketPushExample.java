package com.byb.wallet.sol.remote;

import akka.actor.ActorSystem;
//import akka.actor.typed.ActorSystem;
//import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocket;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;


import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class WebSocketPushExample extends AllDirectives {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("WebSocketServer");

        WebSocketPushExample app = new WebSocketPushExample();
        Route route = app.createRoute();

        final Http http = Http.get(system);
        CompletionStage<ServerBinding> binding = http.newServerAt("localhost", 8080).bind(route);

        System.out.println("Server online at http://localhost:8080/");
    }

    private Route createRoute() {
        return route(
                path("ws", () ->
                        handleWebSocketMessages(webSocketFlow())
                )
        );
    }

    private Flow<Message, Message,?> webSocketFlow() {
        // 定义一个定时发送消息的源
        Source<Message,?> messageSource = Source.tick(Duration.ofSeconds(0), Duration.ofMillis(500), "Hello from server!")
                .map(TextMessage::create);

        // 定义一个丢弃所有接收到消息的接收器
        Sink<Message,?> messageSink = Sink.ignore();

        // 将源和接收器组合成一个流
        return Flow.fromSinkAndSourceCoupled(messageSink, messageSource);
    }
}