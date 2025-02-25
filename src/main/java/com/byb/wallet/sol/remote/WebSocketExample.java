package com.byb.wallet.sol.remote;


import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;
import akka.stream.javadsl.*;
import akka.japi.function.Function2;

import java.util.concurrent.CompletionStage;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer$;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

public class WebSocketExample {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = SystemMaterializer.get(system).materializer();
        final Http http = Http.get(system);

// print each incoming text message
// would throw exception on non strict or binary message
        final Sink<Message, CompletionStage<Done>> printSink =
                Sink.foreach((message) ->
                        System.out.println("Got message: " + message.asTextMessage().getStrictText())
                );

// send this as a message over the WebSocket
        final Source<Message, NotUsed> helloSource =
                Source.single(TextMessage.create("hello world"));

// the CompletionStage<Done> is the materialized value of Sink.foreach
// and it is completed when the stream completes
        final Flow<Message, Message, CompletionStage<Done>> flow =
                Flow.fromSinkAndSourceMat(printSink, helloSource, Keep.left());

        final Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<Done>> pair =
                http.singleWebSocketRequest(
                        WebSocketRequest.create("ws://localhost:8080/ws"),
                        flow,
                        materializer
                );

// The first value in the pair is a CompletionStage<WebSocketUpgradeResponse> that
// completes when the WebSocket request has connected successfully (or failed)
        final CompletionStage<Done> connected = pair.first().thenApply(upgrade -> {
            // just like a regular http request we can access response status which is available via upgrade.response.status
            // status code 101 (Switching Protocols) indicates that server support WebSockets
            if (upgrade.response().status().equals(StatusCodes.SWITCHING_PROTOCOLS)) {
                return Done.getInstance();
            } else {
                throw new RuntimeException("Connection failed: " + upgrade.response().status());
            }
        });

// the second value is the completion of the sink from above
// in other words, it completes when the WebSocket disconnects
        final CompletionStage<Done> closed = pair.second();

// in a real application you would not side effect here
// and handle errors more carefully
        connected.thenAccept(done -> System.out.println("Connected"));
        closed.thenAccept(done -> System.out.println("Connection closed"));
    }
}