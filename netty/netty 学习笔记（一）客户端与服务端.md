# netty 学习笔记（一）客户端与服务端

### 1、添加pom依赖

~~~xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty</artifactId>
    <version>3.3.0.Final</version>
</dependency>
~~~

### 2、服务端创建



~~~java
package com.willow;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Administrator
 * @create 2018-07-27 14:32
 */
public class NettyServer {

    public static void main(String arg[]){
        //1、创建服务器对象
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        //2、创建两个线程池 ，第一个监听端口，nio监听
        ExecutorService boos = Executors.newCachedThreadPool();
        ExecutorService wook = Executors.newCachedThreadPool();
        //3、把两个线程池加入中
        serverBootstrap.setFactory(new NioServerSocketChannelFactory(boos,wook));
        //4、设置管道工厂
        serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            //设置管道
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline channelPipeline=Channels.pipeline();
                channelPipeline.addLast("decoder",new StringDecoder());
                channelPipeline.addLast("encoder",new StringEncoder());
                channelPipeline.addLast("serverHandler",new ServerHanlder());
                //设置事件监听类
                return channelPipeline;
            }
        });

        serverBootstrap.bind(new InetSocketAddress(8080));
        System.out.println("netty 服务器端启动完毕。。。。。。。。。。");

        //访问http://localhost:8080/  查看控制台打印效果
    }
}
class ServerHanlder extends SimpleChannelHandler{

    /**
     * 通道关闭的时候触发
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelClosed(ctx, e);
        System.out.println("threadName:"+Thread.currentThread().getName()+"channelClosed................");
    }

    /**
     * 必须是连接已经建立,关闭通道的时候才会触发.
     */
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        System.out.println("threadName:"+Thread.currentThread().getName()+"channelDisconnected................");
    }

    /**
     * 捕获异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
        System.out.println("threadName:"+Thread.currentThread().getName()+"exceptionCaught................");
    }

    /**
     * 接受消息
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        super.messageReceived(ctx, e);
        System.out.println("messageReceived................");
        System.out.println("threadName:"+Thread.currentThread().getName()+" 服务器接受的消息是: \n"+e.getMessage());
    }
}

~~~

### 3、客户端创建

~~~java
package com.willow;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Administrator
 * @create 2018-07-27 17:30
 */
public class NettyClient {

    public static void main(String arg[]){
        //1、创建服务器对象
        ClientBootstrap clientBootstrap=new ClientBootstrap();
        //2、创建两个线程池 ，第一个监听端口，nio监听
        ExecutorService boos = Executors.newCachedThreadPool();
        ExecutorService wook = Executors.newCachedThreadPool();
        //3、把两个线程池加入中,创建NioClientSocketChannelFactory
        clientBootstrap.setFactory(new NioClientSocketChannelFactory(boos,wook));
        //4、设置管道工厂
        clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            //设置管道
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline channelPipeline=Channels.pipeline();
                channelPipeline.addLast("decoder",new StringDecoder());
                channelPipeline.addLast("encoder",new StringEncoder());
                channelPipeline.addLast("ClientHandler",new ServerHanlder());
                //设置事件监听类
                return channelPipeline;
            }
        });

        //绑定端口
        ChannelFuture connect = clientBootstrap.connect(new InetSocketAddress("127.0.0.1", 8080));
        System.out.println("netty 客户端端启动完毕。。。。。。。。。。");
        Channel channel = connect.getChannel();
        Scanner scanner=new Scanner(System.in);
        while(true){
            System.out.println("请输入内容：");
            channel.write(scanner.next());
        }


    }
}

class ClientHanlder extends SimpleChannelHandler{

    /**
     * 通道关闭的时候触发
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelClosed(ctx, e);
        System.out.println("threadName:"+Thread.currentThread().getName()+"channelClosed................");
    }

    /**
     * 必须是连接已经建立,关闭通道的时候才会触发.
     */
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        System.out.println("threadName:"+Thread.currentThread().getName()+"channelDisconnected................");
    }

    /**
     * 捕获异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
        System.out.println("threadName:"+Thread.currentThread().getName()+"exceptionCaught................");
    }

    /**
     * 接受消息
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        super.messageReceived(ctx, e);
        System.out.println("messageReceived................");
        System.out.println("threadName:"+Thread.currentThread().getName()+" 客户端接受的消息是: \n"+e.getMessage());
        ctx.getChannel().write("hello");//返回
    }
}


~~~



 	