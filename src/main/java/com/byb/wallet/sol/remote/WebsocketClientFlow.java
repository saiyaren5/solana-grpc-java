package com.byb.wallet.sol.remote;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class WebsocketClientFlow {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        Materializer materializer = SystemMaterializer.get(system).materializer();
        Http http = Http.get(system);

// print each incoming text message
// would throw exception on non strict or binary message
        Sink<Message, CompletionStage<Done>> printSink =
                Sink.foreach((message) ->
                        System.out.println("Got message: " + message.asTextMessage().getStrictText())
                );

// send this as a message over the WebSocket
//        Source<Message, NotUsed> helloSource =
//                Source.single(TextMessage.create("hello world"));

//                Source<Message, NotUsed> helloSource = Source.empty();

//        Source<Message,?> emptySource = Source.empty();

//        Source<Message, CompletableFuture<Optional<Message>>> helloSource =
//                Source.single(TextMessage.create("hello world"))
//                        .concatMat(Source.maybe(), Keep.right());

        Source<Message, CompletableFuture<Optional<Message>>> emptySource =
                Source.<Message>single(TextMessage.create("hello world"))
                        .concatMat(Source.<Message>maybe(), Keep.right());




        Flow<Message, Message, CompletionStage<WebSocketUpgradeResponse>> webSocketFlow =
                http.webSocketClientFlow(WebSocketRequest.create("ws://localhost:8080/ws"));


        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<Done>> pair =
                emptySource.viaMat(webSocketFlow, Keep.right())
                        .toMat(printSink, Keep.both())
                        .run(materializer);


// The first value in the pair is a CompletionStage<WebSocketUpgradeResponse> that
// completes when the WebSocket request has connected successfully (or failed)
        CompletionStage<WebSocketUpgradeResponse> upgradeCompletion = pair.first();

// the second value is the completion of the sink from above
// in other words, it completes when the WebSocket disconnects
        CompletionStage<Done> closed = pair.second();

        CompletionStage<Done> connected = upgradeCompletion.thenApply(upgrade->
        {
            System.out.println("xxxxxx");
            // just like a regular http request we can access response status which is available via upgrade.response.status
            // status code 101 (Switching Protocols) indicates that server support WebSockets
            if (upgrade.response().status().equals(StatusCodes.SWITCHING_PROTOCOLS)) {
                return Done.getInstance();
            } else {
                throw new RuntimeException(("Connection failed: " + upgrade.response().status()));
            }
        });

// in a real application you would not side effect here
// and handle errors more carefully
        connected.thenAccept(done -> System.out.println("Connected"));
//        closed.thenAccept(done -> System.out.println("Connection closed"));
    }
}
