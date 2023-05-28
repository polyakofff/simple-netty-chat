package org.example;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(this.getClass());
    private WebSocketServerHandshaker handshaker;
    private final MessageHandler messageHandler;

    public MyWebSocketHandler(MessageHandler messageHandler) {
        super();
        this.messageHandler = messageHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("read0");

        if (msg instanceof FullHttpRequest request)
            handleHttpRequest(ctx, request);
        else if (msg instanceof WebSocketFrame frame)
            handleWebSocketFrame(ctx, frame);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.debug("read complete");

//        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        logger.info("http handshake request");

        // bad request
        if (!request.decoderResult().isSuccess()) {
            sendHttpResponse(ctx,
                    new DefaultFullHttpResponse(
                            request.protocolVersion(),
                            HttpResponseStatus.BAD_REQUEST
                    )
            );
            return;
        }

        // only GET requests
        if (request.method() != HttpMethod.GET) {
            sendHttpResponse(ctx,
                    new DefaultFullHttpResponse(
                            request.protocolVersion(),
                            HttpResponseStatus.FORBIDDEN
                    )
            );
            return;
        }

        // handshake
        handshaker = new WebSocketServerHandshakerFactory(
                getWebSocketUrl(request),
                null,
                true
        )
                .newHandshaker(request);
        if (handshaker == null)
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        else
            handshaker.handshake(ctx.channel(), request);
        logger.info("handshake was successful");
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
        ChannelFuture future = ctx.channel().write(response);
        if (response.status().code() != 200)
            future.addListener(ChannelFutureListener.CLOSE);
    }

    private String getWebSocketUrl(FullHttpRequest request) {
        return "ws://" + request.headers().get(HttpHeaderNames.HOST) + "/websocket";
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            logger.debug("close frame");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            logger.debug("ping frame");
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof TextWebSocketFrame textFrame) {
            logger.debug("text frame");
            messageHandler.handle(textFrame.text(), new User(ctx.channel()));
            return;
        }

        if (frame instanceof BinaryWebSocketFrame) {
            logger.debug("binary frame");
        }
    }
}
