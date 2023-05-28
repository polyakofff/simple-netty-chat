package org.example;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class User {

    private final Channel channel;

    public User(Channel channel) {
        this.channel = channel;
    }

    public synchronized void send(String msg) {
        channel.writeAndFlush(new TextWebSocketFrame(msg));
    }

    public void setLogoutAction(Runnable logoutAction) {
        channel.closeFuture()
                .addListener((ChannelFutureListener) future -> logoutAction.run());
    }

}
