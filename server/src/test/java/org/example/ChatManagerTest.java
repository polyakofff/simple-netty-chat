package org.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatManagerTest {

    synchronized void log(String msg) {
        Thread curr = Thread.currentThread();
        System.out.println("[" + curr.getId() + "] " + msg);
    }

    @Test
    void testMultiUsers() throws InterruptedException {
        ChatManager chatManager = new ChatManager();

        int nUsers = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(nUsers);
        CountDownLatch latch = new CountDownLatch(nUsers);
        for (int i = 0; i < nUsers; i++) {
            String fromUser = "user" + i;
            String message = "{ \"username\": \"" + fromUser + "\" }";

            executorService.execute(() -> {
                EmbeddedChannel channel = new EmbeddedChannel(
                        new MyWebSocketHandler(chatManager)
                );

                channel.writeInbound(new TextWebSocketFrame(message));

                for (Object msg : channel.outboundMessages()) {
                    TextWebSocketFrame frame = (TextWebSocketFrame) msg;
                    log(frame.text());
                }

                latch.countDown();
            });
//            Thread.sleep(500);
        }

        latch.await();
        assertEquals(nUsers, chatManager.getActiveUsers().size());
        assertTrue(chatManager.getActiveUsers().size() <= ChatManager.MAX_USERS);
        assertTrue(chatManager.getMessageHistory().size() <= ChatManager.MAX_MESSAGES);

    }
}
