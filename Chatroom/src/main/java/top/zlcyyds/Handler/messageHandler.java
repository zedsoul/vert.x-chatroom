package top.zlcyyds.Handler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class messageHandler extends AbstractVerticle {
    @Override
    public void start() throws Exception {
       vertx.eventBus().consumer("chatroom",message->{
           String msg = message.body().toString();
           System.out.println("Received message:"+msg);
       });
    }
}
