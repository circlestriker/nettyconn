package com.striker.nettyconn;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import static java.lang.Thread.sleep;

public class Client {

    private NioEventLoopGroup worker = new NioEventLoopGroup();

    private Channel channel;

    private Bootstrap bootstrap;

    public static void main(String[] args) throws InterruptedException {
        Client  client = new Client();

        client.start();

        client.sendData();
        // ---- client /127.0.0.1:55264 reader timeOut, --- close it
        // ---/127.0.0.1:55264----- channel is Inactive
        client.sendProtoData();
    }

    private void start() {
        bootstrap = new Bootstrap();        
        bootstrap.group(worker)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                // TODO Auto-generated method stub
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast(new IdleStateHandler(0,0,5));

                pipeline.addLast(new MsgPckDecode());

                pipeline.addLast(new MsgPckEncode());

                pipeline.addLast(new Client3Handler(Client.this));              
            }           
        }); 
        doConnect();
    }

    /**
     * 连接服务端 and 重连
     */
    protected void doConnect() {

        if (channel != null && channel.isActive()){
            return;
        }       
        ChannelFuture connect = bootstrap.connect("127.0.0.1", 8081);
        //实现监听通道连接的方法
        connect.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {

                if(channelFuture.isSuccess()){
                    channel = channelFuture.channel();
                    System.out.println("客户端|连接成功");
                    //sendData(); //这里发为什么会导致连接中断?
                }else{
                    System.out.println("每隔2s重连....");
                    channelFuture.channel().eventLoop().schedule(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            doConnect();
                        }
                    },2,TimeUnit.SECONDS);
                }   
            }
        });     
    }   
    /**
     * 向服务端发送消息
     */
    private void sendData() throws InterruptedException {
        sleep(1000);
        Scanner sc= new Scanner(System.in); 
        for (int i = 0; i < 1000; i++) {

            if(channel != null && channel.isActive()){              
                //获取一个键盘扫描器
                String nextLine = sc.nextLine();
                Model model = new Model();

                model.setType(TypeData.CUSTOMER);

                model.setBody(nextLine);

                channel.writeAndFlush(model);
            }
        }
    }

    /**
     * 向服务端发送消息
     */
    private void sendProtoData() throws InterruptedException {
        sleep(1000);
        Scanner sc= new Scanner(System.in);
        for (int i = 0; i < 1000; i++) {
            if(channel != null && channel.isActive()){
                //1、 创建Builder
                MessageProto.Message.Builder builder = MessageProto.Message.newBuilder();
                //2、 设置Person的属性
                builder.setId("id"+i);
                builder.setType(0);
                builder.setContent("hello|"+i);
                //3、 创建
                MessageProto.Message message = builder.build();
                //4、序列化
                //5、将data保存在本地或者是传到网络
                channel.writeAndFlush(message);
            }
        }
    }
}
