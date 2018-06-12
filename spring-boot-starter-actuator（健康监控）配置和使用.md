spring-boot-starter-actuator（健康监控）配置和使用





添加POM依赖：

```xml
<!-- spring-boot-监控-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

application.yml中指定监控的HTTP端口（如果不指定，则使用和Server相同的端口）；指定去掉某项的检查（比如不监控health.mail）： 

~~~yaml
server:
  port: 8083
management:
    port: 8083
    security:
      enabled: false  #
~~~

监控和管理端点 


| 端点名      | 描述                                                         |
| ----------- | ------------------------------------------------------------ |
| autoconfig  | 所有自动配置信息（ positiveMatches :运行的， negativeMatches 未运行组件） |
| auditevents | 审计事件                                                     |
| beans       | 所有Bean的信息                                               |
| configprops | 所有配置属性                                                 |
| dump        | 线程状态信息                                                 |
| env         | 当前环境信息                                                 |
| health      | 应用健康状况                                                 |
| info        | 当前应用信息                                                 |
| metrics     | 应用的各项指标                                               |
| mappings    | 应用@RequestMapping映射路径                                  |
| shutdown    | 关闭当前应用（默认关闭）                                     |
| trace       | 追踪信息（最新的http请求）                                   |
| heapdump       | 下载内存快照                             |
http://localhost:8083/info 读取配置文件application.properties的 info.*属性

  在InfoProperties 读取

  application.properties :

```
info.app.version=v1.2.0
info.app.name=abc
```

 在GitProperties  获取git.properties 的信息  

~~~properties
info.app.version=v1.2.0
info.app.name=abc
#远程关闭开启
endpoints.shutdown.enabled=true  
#访问：http://localhost:8083/shutdown   关闭服务
~~~

metrics 

~~~json
{
mem: 573549,   //内存大小
mem.free: 388198,  //内存剩余大小
processors: 4,  //处理器数量
instance.uptime: 338426,
uptime: 345091,
systemload.average: -1,
heap.committed: 489984,
heap.init: 131072,
heap.used: 101785,
heap: 1842688,
nonheap.committed: 85056,
nonheap.init: 2496,
nonheap.used: 83566,
nonheap: 0,
threads.peak: 46,
threads.daemon: 36,
threads.totalStarted: 72,
threads: 39,  //线程
classes: 12109,
classes.loaded: 12109,  //加载的类
classes.unloaded: 0,  //没加载的类
gc.ps_scavenge.count: 10,
gc.ps_scavenge.time: 103,
gc.ps_marksweep.count: 3,
gc.ps_marksweep.time: 219,
httpsessions.max: -1,
httpsessions.active: 0,
gauge.response.mappings: 3,
gauge.response.autoconfig: 4,
gauge.response.trace: 167,
counter.status.200.mappings: 1,
counter.status.200.autoconfig: 2,
counter.status.200.trace: 1
}
~~~
自定义配置说明：
~~~properties
#关闭metrics功能
endpoints.metrics.enabled=false
#开启shutdown远程关闭功能
endpoints.shutdown.enabled=true
#设置beansId
endpoints.beans.id=mybean
#设置beans路径
endpoints.beans.path=/bean
#关闭beans 功能
endpoints.beans.enabled=false
#关闭所有的
endpoints.enabled=false 
#开启单个beans功能
endpoints.beans.enabled=true
#所有访问添加根目录
management.context-path=/manage

management.port=8181

~~~


org.springframework.boot.actuate.health 包下对于所有的健康状态检查
例如：RedisHealthIndicator ，当有redis的starter 时候就会检查

```json
 {
    status: "DOWN", //状态
    diskSpace: {
    status: "UP",
    total: 395243941888,
    free: 367246643200,
    threshold: 10485760
    },
    rabbit: {
    status: "DOWN",
    error: "org.springframework.amqp.AmqpConnectException: java.net.ConnectException: Connection refused: connect"
    },
    redis: {
    status: "UP",
    version: "4.0.9"
    },
    db: {
    status: "UP",
    database: "MySQL",
    hello: 1
    }
}
```
自定义health
 
 * 自定义健康状态指示器

 * 1、编写一个指示器 实现 HealthIndicator 接口

 * 2、指示器的名字 xxxxHealthIndicator

 * 3、加入容器中

     ~~~java
    import org.springframework.boot.actuate.health.Health;
    import org.springframework.boot.actuate.health.HealthIndicator;
    import org.springframework.stereotype.Component;
    @Component
    public class MyAppHealthIndicator implements HealthIndicator {
    
        @Override
        public Health health() {
    
            //自定义的检查方法
            //Health.up().build()代表健康
            return Health.down().withDetail("msg","服务异常").build();
        }
    }
     ~~~

    