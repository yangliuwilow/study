# Dubbo 源码学习笔记（二） ServiceBean分析



​       当Spring容器处理完`<dubbo:service>`标签后，会在Spring容器中生成一个**ServiceBean** ，服务的发布也会在`ServiceBean`中完成。不妨看一下`ServiceBean`的定义 

~~~java
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, BeanNameAware {
}
~~~



​     而在Spring初始化完成Bean的组装，会调用**InitializingBean**的`afterPropertiesSet`方法，在Spring容器加载完成，会接收到事件**ContextRefreshedEvent**，调用**ApplicationListener**的`onApplicationEvent`方法。

在`afterPropertiesSet`中，和`onApplicationEvent`中，都会调用`export()`，在`export()`中，会暴露dubbo服务，具体区别在于是否配置了`delay`属性，是否延迟暴露，

- 如果delay不为`null`，或者不为`-1`时，会在`afterPropertiesSet`中调用`export()`暴露dubbo服务，
- 如果为`null`,或者为`-1`时，会在Spring容器初始化完成，接收到**ContextRefreshedEvent**事件，调用`onApplicationEvent`，暴露dubbo服务。

### ServiceBean 主要方法：

 ~~~java
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, BeanNameAware {
    //Spring容器初始化完成，调用
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (isDelay() && !isExported() && !isUnexported()) {
            if (logger.isInfoEnabled()) {
                logger.info("The service ready on spring started. service: " + getInterface());
            }
            //暴露服务
            export();
        }
    }
    //判断是否延迟发布
    private boolean isDelay() {
        Integer delay = getDelay();
        ProviderConfig provider = getProvider();
        if (delay == null && provider != null) {
            delay = provider.getDelay();
        }
        return supportedApplicationListener && (delay == null || delay == -1);
    }
    //当bean初始化完成调用
    public void afterPropertiesSet() throws Exception {
        //......此处省略10000行代码
        if (!isDelay()) {
            //暴露服务
            export();
        }
    }
}
 ~~~

在`export()`，暴露服务过程中，如果发现有`delay属性`，则延迟`delay时间`，暴露服务，如果没有，则直接暴露服务。 

~~~java
public synchronized void export() {
    //忽略若干行代码
    if (delay != null && delay > 0) {
        //当delay不为null，且大于0时，延迟delay时间，暴露服务
        delayExportExecutor.schedule(new Runnable() {
            public void run() {
                //暴露服务
                doExport();
            }
        }, delay, TimeUnit.MILLISECONDS);
    } else {
        //直接暴露服务
        doExport();
    }
} 
~~~

而在`doExport()`中，验证参数，按照不同的`Protocol`，比如(`dubbo`,`injvm`)暴露服务，在不同的`zookeeper`集群节点上注册自己的服务。 

 ~~~java
protected synchronized void doExport() {
    //忽略10000行代码
    doExportUrls();
    //忽略10000行代码
}

private void doExportUrls() {
    List<URL> registryURLs = loadRegistries(true);
    for (ProtocolConfig protocolConfig : protocols) {
        //按照不同的Protocal暴露服务   protocolConfig =<dubbo:protocol name="dubbo" port="20880" id="dubbo" />
        doExportUrlsFor1Protocol(protocolConfig, registryURLs);
    }
} 
 ~~~



### 1、组装Url创建代理invoker对象

这里以`dubbo`协议为例，看一下发布的过程,在发布过程中，会用一个变量`map`保存URL的所有变量和value值,然后调用代理工程proxyFactory，获取代理类，然后将invoker转换成exporter，暴露服务，具体如下： 

```java
 protocol://host:port/path?key=value&key=value
```

~~~java
private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
    //如果协议类型为null，则默认为dubbo协议
    String name = protocolConfig.getName();
    if (name == null || name.length() == 0) {
        name = "dubbo";
    }
    //map是保存url中key-Value的值
    Map<String, String> map = new HashMap<String, String>();
    //URL中的side属性，有两个值，一个provider，一个consumer，暴露服务的时候为provider
    map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
    //dubbo的版本号  url中的dubbo
    map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
    //url中的timestamp
    map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
    //url中的pid
    if (ConfigUtils.getPid() > 0) {
        map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
    }
    //从其他参数中获取参数
    appendParameters(map, application);
    appendParameters(map, module);
    appendParameters(map, provider, Constants.DEFAULT_KEY);
    appendParameters(map, protocolConfig);
    appendParameters(map, this);
    //忽略若干代码

    if (ProtocolUtils.isGeneric(generic)) {
        map.put("generic", generic);
        map.put("methods", Constants.ANY_VALUE);
    } else {
        //url中的revesion字段
        String revision = Version.getVersion(interfaceClass, version);
        if (revision != null && revision.length() > 0) {
            map.put("revision", revision);
        }
        //拼接URL中的methods
        String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
        if (methods.length == 0) {
            logger.warn("NO method found in service interface " + interfaceClass.getName());
            map.put("methods", Constants.ANY_VALUE);
        } else {
            map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
        }
    }
    //token 临牌校验
    if (!ConfigUtils.isEmpty(token)) {
        if (ConfigUtils.isDefault(token)) {
            map.put("token", UUID.randomUUID().toString());
        } else {
            map.put("token", token);
        }
    }
    //injvm协议
    if ("injvm".equals(protocolConfig.getName())) {
        protocolConfig.setRegister(false);
        map.put("notify", "false");
    }
    //获取上下文路径
    String contextPath = protocolConfig.getContextpath();
    if ((contextPath == null || contextPath.length() == 0) && provider != null) {
        contextPath = provider.getContextpath();
    }
    //获取主机名
    String host = this.findConfigedHosts(protocolConfig, registryURLs, map);
    //获取端口
    Integer port = this.findConfigedPorts(protocolConfig, name, map);
    //通过map组装URL
    URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);
    //如果url使用的协议存在扩展，调用对应的扩展来修改原url
    if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
        .hasExtension(url.getProtocol())) {
        url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
            .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
    }

    String scope = url.getParameter(Constants.SCOPE_KEY);
    //配置为none不暴露
    if (!Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {
        if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
            //如果不是remote，则暴露本地服务
            exportLocal(url);
        }
        //如果配置不是local则暴露为远程服务
        if (!Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope)) {
            // 如果注册中心地址不为null
            if (registryURLs != null && registryURLs.size() > 0) {
                 //循环多个注册中心，进行暴露
                for (URL registryURL : registryURLs) {
                    url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));
                    //忽略不相干的代码  
                    // 通过代理工厂将ref对象转化成invoker对象
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                    //代理invoker对象
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
                    // 暴露服务
                    Exporter<?> exporter = protocol.export(wrapperInvoker);
                    //将创建的exporter放进链表便于管理  
                    exporters.add(exporter);
                }
            } else {
                Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);
                DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                Exporter<?> exporter = protocol.export(wrapperInvoker);
                exporters.add(exporter);
            }
        }
    }
    this.urls.add(url);
}

~~~



### 2、protocol.export()服务暴露

​      Dubbo 缺省协议采用单一长连接和 NIO 异步通讯，适合于小数据量大并发的服务调用，以及服务消费者机器数远大于服务提供者机器数的情况。

反之，Dubbo 缺省协议不适合传送大数据量的服务，比如传文件，传视频等，除非请求量很低。

 调用DubboProtocol.export()的方法



```java
//export:222, DubboProtocol
public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    URL url = invoker.getUrl();

    
     //打开服务
    openServer(url);
    optimizeSerialization(url);
    return exporter;
}
//打开服务
private void openServer(URL url) {
    // find server.
    String key = url.getAddress();
    //是否server端
    boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
    if (isServer) {
        ExchangeServer server = serverMap.get(key);
        if (server == null) {
            //如果服务不存在，创建服务
            serverMap.put(key, createServer(url));
        } else {
            //如果服务存在，覆盖这个服务
            server.reset(url);
        }
    }
}

//创建服务
private ExchangeServer createServer(URL url) {
    //忽略若干代码
    ExchangeServer server;
    try {
        server = Exchangers.bind(url, requestHandler);
    } catch (RemotingException e) {
        throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
    }
    return server;
}
 
```





而在`headerExchanger`的`bind`中，调用了`Transporters.bind()`,一直调用到`NettyServer`,绑定了端口和链接。

```java
public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
     return new HeaderExchangeServer(Transporters.bind(url, new DecodeHandler(new HeaderExchangeHandler(handler))));
}

//Transporters.bind
public static Server bind(URL url, ChannelHandler... handlers) throws RemotingException {
//忽略很多代码
return getTransporter().bind(url, handler);
}
//上段代码的getTransporter()
public static Transporter getTransporter() {
return ExtensionLoader.getExtensionLoader(Transporter.class).getAdaptiveExtension();
}
```

 而在Transporter的定义中看到下面代码：

```java
@SPI("netty")
public interface Transporter {
    @Adaptive({Constants.SERVER_KEY, Constants.TRANSPORTER_KEY})
    Server bind(URL url, ChannelHandler handler) throws RemotingException;
    @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    Client connect(URL url, ChannelHandler handler) throws RemotingException;
}
```

所以这里调用的是`NettyTransporter`,这里启动了一个新的`NettyServer`。

```java
public class NettyTransporter implements Transporter {

    public static final String NAME = "netty4";

    public Server bind(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyServer(url, listener);
    }

    public Client connect(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyClient(url, listener);
    }
}
```

在NettyServer的构造方法中，调用了父类的构造方法，调用了`doOpen()`方法指定了端口

```java
public class NettyServer extends AbstractServer implements Server {
  public NettyServer(URL url, ChannelHandler handler) throws   RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }
}

public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
       super(url, handler);
       //忽略很多代码  
      doOpen();
       //忽略很多代码
}


 @Override
    protected void doOpen() throws Throwable {
        NettyHelper.setNettyLoggerFactory();

        bootstrap = new ServerBootstrap();

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        workerGroup = new NioEventLoopGroup(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                new DefaultThreadFactory("NettyServerWorker", true));

        final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
        channels = nettyServerHandler.getChannels();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                                .addLast("decoder", adapter.getDecoder())
                                .addLast("encoder", adapter.getEncoder())
                                .addLast("handler", nettyServerHandler);
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();

    }
```

 

 





 ![dubbo-serverBean](..\images\dubbo-serverBean.png)

 

 

 



 

 https://blog.csdn.net/mrzhangxl/article/details/76347451

https://blog.csdn.net/peace_hehe/article/category/7449148

http://dubbo.apache.org/#!/docs/dev/design.md?lang=zh-cn

https://blog.csdn.net/meilong_whpu/article/category/7500700

https://www.jianshu.com/p/7f3871492c71

 

https://blog.csdn.net/flashflight/article/details/44318447#comments