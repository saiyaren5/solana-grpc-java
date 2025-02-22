package com.byb.wallet.sol.remote;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebsocketClientTest {

    public static void main(String[] args) throws Exception {
        new WebsocketClientTest("ws://localhost:18197/websocket-endpoint").connect();
    }

    private WebSocketClient webSocketClient;

    public WebsocketClientTest(String serverUri) throws URISyntaxException {
        webSocketClient = new WebSocketClient(new URI(serverUri), new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                // 连接建立后，发送 STOMP 连接帧
                send("CONNECT\n\n\0");
                // 订阅主题
//                subscribe("/topic/messages/fdfd");
                subscribe("/topic/trade");
            }

            @Override
            public void onMessage(String message) {
                // 收到消息
                System.out.println("Received message: " + message);
                // 处理收到的消息
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                // 连接关闭
                System.out.println("Connection closed. Code: " + code + ", Reason: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                // 发生错误
                ex.printStackTrace();
            }
        };
    }

    public void connect() {
        webSocketClient.connect();
    }

    public void disconnect() {
        webSocketClient.close();
    }

    private void subscribe(String topic) {
        String frame = "SUBSCRIBE\n" +
                "destination:" + topic + "\n" +
                "id:sub-12321\n" +
                "ack:auto\n\n\0";
        webSocketClient.send(frame);
    }
//    private void subscribe(String topic) {
//        String frame = "SUBSCRIBE\n" +
//                "destination:" + topic + "\n" +
//                "id:sub-12321\n" +
//                "ack:auto\n\n\0";
//        webSocketClient.send(frame);
}
