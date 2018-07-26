# java网络编程之一URL编程！

URL（Uniform Resource Locator）中文名为统一资源定位符，有时也被俗称为网页地址。表示为互联网上的资源，如网页或者FTP地址。 

~~~java
package com.willow.net;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

//URL：统一资源定位符，一个URL的对象，对应着互联网上一个资源   ，我们可以通过URL的对象调用器相应的方法，将此资源读取下载
public class TestURL {
    @Test
    public void url(){
        try {
            URL url =new URL("https://www.baidu.com/");
            url.getProtocol() ;
            System.out.println("获取该URL的协议名:"+url.getProtocol());
            System.out.println("获取该URL的主机名:"+url.getHost());
            System.out.println("获取该URL的端口号，如果没有设置端口，返回-1:"+url.getPort());

            System.out.println("获取该URL的文件名，如果没有返回空串:"+url.getFile());
            System.out.println("获取该URL中记录的引用，如果URL不含引用，返回null:"+url.getRef());
            System.out.println("获取该URL的查询信息:"+url.getQuery());

            System.out.println("获取该URL的路径:"+url.getPath());
            System.out.println("获取该URL的权限信息:"+url.getAuthority());
            System.out.println("获得使用者的信息:"+url.getUserInfo());

            //方法openStream()与指定的URL建立连接并返回InputStream类的对象以从这一连接中读取数据
            InputStream inputStream = url.openStream();
            byte [] bytes=new byte[200];
            int len;
            while((len=inputStream.read(bytes))!=-1){
                String str=new String(bytes,0,len);
                System.out.println("###"+str);
            }
            inputStream.close();

            //利用URLConnection实现双向通信  ,即可输入，也可输出
            URLConnection content = url.openConnection();
            InputStream inputStream1 = content.getInputStream();
            //读取数据输出到 本地abc.txt中
            OutputStream outputStream=new FileOutputStream(new File("abc.txt"));
            byte[] bytess=new byte[500];
            int length;
            while((length=inputStream1.read(bytess))!=-1){
                outputStream.write(bytess,0,length);
            }
            outputStream.close();
            inputStream1.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
~~~









| 序号 | 方法描述                                                     |
| ---- | ------------------------------------------------------------ |
| 1    | **public URL(String protocol, String host, int port, String file) throws MalformedURLException.** 通过给定的参数(协议、主机名、端口号、文件名)创建URL。 |
| 2    | **public URL(String protocol, String host, String file) throws MalformedURLException** 使用指定的协议、主机名、文件名创建URL，端口使用协议的默认端口。 |
| 3    | **public URL(String url) throws MalformedURLException** 通过给定的URL字符串创建URL |
| 4    | **public URL(URL context, String url) throws MalformedURLException** 使用基地址和相对URL创建 |


URL类部分方法

| 方法                  | 说明                                             |
| --------------------- | ------------------------------------------------ |
| String getPotocol()   | 获取该URL的协议名                                |
| String getHost()      | 获取该URL的主机名                                |
| Int getPort()         | 获取该URL的端口号，如果没有设置端口，返回-1      |
| String getFile()      | 获取该URL的文件名，如果没有返回空串              |
| String getRef()       | 获取该URL中记录的引用，如果URL不含引用，返回null |
| String getQuery()     | 获取该URL的查询信息                              |
| String getPath()      | 获取该URL的路径                                  |
| String getAuthority() | 获取该URL的权限信息                              |
| String getUserInfo()  | 获得使用者的信息                                 |

## URLConnections 类方法

openConnection() 返回一个 java.net.URLConnection。

例如：

- 如果你连接HTTP协议的URL, openConnection() 方法返回 HttpURLConnection 对象。
- 如果你连接的URL为一个 JAR 文件, openConnection() 方法将返回 JarURLConnection 对象。
- 等等...

URLConnection 方法列表如下：

| 序号 | 方法描述                                                     |
| ---- | ------------------------------------------------------------ |
| 1    | **Object getContent()**  检索URL链接内容                     |
| 2    | **Object getContent(Class[] classes)**  检索URL链接内容      |
| 3    | **String getContentEncoding()**  返回头部 content-encoding 字段值。 |
| 4    | **int getContentLength()**  返回头部 content-length字段值    |
| 5    | **String getContentType()**  返回头部 content-type 字段值    |
| 6    | **int getLastModified()**  返回头部 last-modified 字段值。   |
| 7    | **long getExpiration()**  返回头部 expires 字段值。          |
| 8    | **long getIfModifiedSince()**  返回对象的 ifModifiedSince 字段值。 |
| 9    | **public void setDoInput(boolean input)** URL 连接可用于输入和/或输出。如果打算使用 URL 连接进行输入，则将 DoInput 标志设置为 true；如果不打算使用，则设置为 false。默认值为 true。 |
| 10   | **public void setDoOutput(boolean output)** URL 连接可用于输入和/或输出。如果打算使用 URL 连接进行输出，则将 DoOutput 标志设置为 true；如果不打算使用，则设置为 false。默认值为 false。 |
| 11   | **public InputStream getInputStream() throws IOException** 返回URL的输入流，用于读取资源 |
| 12   | **public OutputStream getOutputStream() throws IOException** 返回URL的输出流, 用于写入资源。 |
| 13   | **public URL getURL()** 返回 URLConnection 对象连接的URL     |