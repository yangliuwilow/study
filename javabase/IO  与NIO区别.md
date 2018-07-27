IO  与NIO区别

IO (BIO)   ： 同步、阻塞IO

NIO         ：（jdk1.7之前）同步、非阻塞IO

jdk1.7之后AIO   ：异步、非阻塞IO





IO  与NIO区别：阻塞和非阻塞,  通道+选择器



~~~java
package com.willow.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;

/*
 * 一、使用 NIO 完成网络通信的三个核心：
 *
 * 1. 通道（Channel）：负责连接
 *
 * 	   java.nio.channels.Channel 接口：
 * 			|--SelectableChannel
 * 				|--SocketChannel
 * 				|--ServerSocketChannel
 * 				|--DatagramChannel
 *
 * 				|--Pipe.SinkChannel
 * 				|--Pipe.SourceChannel
 *
 * 2. 缓冲区（Buffer）：负责数据的存取
 *
 * 3. 选择器（Selector）：是 SelectableChannel 的多路复用器。用于监控 SelectableChannel 的 IO 状况
 *
 */

//NIO 客户端
public class NioClient {

    public static void main(String arg[]) {
        System.out.println("客户端已经启动.............");
        try {
            //1、创建socker通道
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8080));
            //2、切换异步非阻塞
            socketChannel.configureBlocking(false); //1.7及以上
            //3、指定缓冲区大小
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            byteBuffer.put(new Date().toString().getBytes());
            //4、切换到读取模式
            byteBuffer.flip();
            //5、写入到缓冲区
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            //6、关闭通道
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



//NIO 服务端
class NioServer {


    public static void main(String arg[]) {

        System.out.println("服务器端启动");
        try {
            //1、创建服务器端通道
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            //2、切换异步非阻塞
            serverSocketChannel.configureBlocking(false); //1.7及以上
            //3、切换读取模式
            serverSocketChannel.bind(new InetSocketAddress(8080));
            //4、获取选择器
            Selector selector = Selector.open();
            //5、将通道注册到选择器中去，并且监听已经收到的事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            //6、轮询获取“已经准备就绪的”的事件
            while (selector.select() > 0) {
                //7、获取当前选择器中所有注册的“选择键(已就绪的监听事件)”
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    //8、获取准备“就绪”的是事件
                    SelectionKey sk = iterator.next();
                    //9、 判断具体是什么事件准备就绪
                    if (sk.isAcceptable()) {
                        //10、 若“接收就绪”，获取客户端连接
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        //11、 切换非阻塞模式
                        socketChannel.configureBlocking(false);
                        //12、 将该通道注册到选择器上
                        socketChannel.register(selector, SelectionKey.OP_READ);


                    } else if (sk.isReadable()){
                        //13、 获取当前选择器上“读就绪”状态的通道
                        SocketChannel sChannel = (SocketChannel) sk.channel();

                        //14、 指定缓冲区大小，读取数据
                        ByteBuffer buf = ByteBuffer.allocate(1024);

                        int len = 0;
                        while ((len = sChannel.read(buf)) > 0) {  //sChannel 读取数据到ByteBuffer
                            buf.flip(); //切换到读取模式
                            String str = new String(buf.array(), 0, len);  //读取
                            System.out.println("str:"+str);
                            buf.clear();
                        }

                    }

                    //15. 取消选择键 SelectionKey
                    iterator.remove();

                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

~~~

