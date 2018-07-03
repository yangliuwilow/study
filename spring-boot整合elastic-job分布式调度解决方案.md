## Spring-boot整合elastic-job分布式调度解决方案 

文档地址：http://elasticjob.io/docs/elastic-job-lite/00-overview/intro/

源码地址：https://github.com/elasticjob

源码解析：https://blog.csdn.net/spy19881201/article/details/61631799

## 一、Spring boot 整合

#### **1. 添加依赖【在此只列出额外需要添加的elastic-job依赖的jar】**

```xml
<!-- ElasticJobAutoConfiguration自动配置类作用-->
<dependency>
    <groupId>com.github.kuhn-he</groupId>
    <artifactId>elastic-job-lite-spring-boot-starter</artifactId>  
    <version>2.1.5</version>
</dependency>
```

#### **2. 添加相应配置项**

```properties
elaticjob.zookeeper.server-lists=10.140.6.18:2181,10.140.6.19:2181
elaticjob.zookeeper.namespace=my-project
```

#### **3. 创建定时任务**

```java
import com.dangdang.elasticjob.lite.annotation.ElasticSimpleJob;
import org.springframework.stereotype.Component;
import com.dangdang.ddframe.job.api.ShardingContext;

@ElasticSimpleJob(cron = "* * * * * ?", jobName = "test123", shardingTotalCount = 2, jobParameter = "测试参数", shardingItemParameters = "0=A,1=B")
@Component
public class MySimpleJob implements com.dangdang.ddframe.job.api.simple.SimpleJob {

    @Override
    public void execute(ShardingContext shardingContext) {
        System.out.println(String.format("------Thread ID: %s, 任务总片数: %s, " +
                        "当前分片项: %s.当前参数: %s," +
                        "当前任务名称: %s.当前任务参数: %s"
                ,
                Thread.currentThread().getId(),
                shardingContext.getShardingTotalCount(),
                shardingContext.getShardingItem(),
                shardingContext.getShardingParameter(),
                shardingContext.getJobName(),
                shardingContext.getJobParameter()

        ));

    }
}
```

#### **4. 启动2个不同端口，查看执行结果** 

  执行： java -jar  xxx.jar  --server.port=8081

```xml
------Thread ID: 83, 任务总片数: 2, 当前分片项: 0.当前参数: A,当前任务名称: com.willow.elasticJob.MySimpleJob.当前任务参数: 
------Thread ID: 84, 任务总片数: 2, 当前分片项: 1.当前参数: B,当前任务名称: com.willow.elasticJob.MySimpleJob.当前任务参数: 
------Thread ID: 89, 任务总片数: 2, 当前分片项: 0.当前参数: A,当前任务名称: com.willow.elasticJob.MySimpleJob.当前任务参数: 
------Thread ID: 90, 任务总片数: 2, 当前分片项: 1.当前参数: B,当前任务名称: com.willow.elasticJob.MySimpleJob.当前任务参数: 
```

------

### 1.2动态添加elastic-job任务

当前暂未解决的问题： 动态添加的任务只能在添加的机器上运行，平行部署的其他机器上不会运行该任务

在上边配置的基础上添加以下配置：

#### **1. 添加zookeeper配置类，和动态添加方法**

```java
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
@Configuration
public class ElasticJobConfig {

    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter regCenter(@Value("${elaticjob.zookeeper.server-lists}") final String serverList, @Value("${elaticjob.zookeeper.namespace}") final String namespace) {
        return new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
    }

    @Autowired
    private ZookeeperRegistryCenter regCenter;

    /**
     * 动态添加
     * @param jobClass
     * @param cron
     * @param shardingTotalCount
     * @param shardingItemParameters
     */
    public void addSimpleJobScheduler(final Class<? extends SimpleJob> jobClass,
                                      final String cron,
                                      final int shardingTotalCount,
                                      final String shardingItemParameters){
        JobCoreConfiguration coreConfig = JobCoreConfiguration.newBuilder(jobClass.getName(), cron, shardingTotalCount).shardingItemParameters(shardingItemParameters).jobParameter("job参数").build();
        SimpleJobConfiguration simpleJobConfig = new SimpleJobConfiguration(coreConfig, jobClass.getCanonicalName());
        JobScheduler jobScheduler = new JobScheduler(regCenter, LiteJobConfiguration.newBuilder(simpleJobConfig).build());
        jobScheduler.init();

    }
}
```

#### **2. 动态添加任务逻辑**

```java
import com.willow.elasticJob.MySimpleJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private ElasticJobConfig elasticJobConfig;

    @RequestMapping("/addJob")
    public void addJob() {
        int shardingTotalCount = 2;
        elasticJobConfig.addSimpleJobScheduler(new MySimpleJob().getClass(),"* * * * * ?",shardingTotalCount,"0=A,1=B");

    }

}
```


## 二、Spring 集成 elastic-job

####   2.1添加POM依赖

~~~xml
<!-- elastic-job -->
<dependency>
    <groupId>com.dangdang</groupId>
    <artifactId>elastic-job-lite-core</artifactId>
    <version>2.1.5</version>
</dependency>
<dependency>
    <groupId>com.dangdang</groupId>
    <artifactId>elastic-job-lite-spring</artifactId>
    <version>2.1.5</version>
</dependency>
<!-- elastic-job  end -->
~~~



####   2.2添加配置

```properties
server.port=8766
spring.application.name=scheduler-service
#  zookeeper注册中心
spring.elasticjob.serverList = 192.168.7.108:2181
spring.elasticjob.namespace = elastic-job-lite-springboot 

#
stockJob.cron = 0/5 * * * * ?
stockJob.shardingTotalCount = 2
stockJob.shardingItemParameters = 0=Chengdu0,1=Chengdu1
```

#### 2.3 添加zookeeper注册中心

~~~java
package com.willow.config;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${spring.elasticjob.serverList}'.length() > 0") //判断是否配置了zookeeper 地址
public class JobRegistryCenterConfig {

    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter regCenter(@Value("${spring.elasticjob.serverList}") final String serverList, @Value("${spring.elasticjob.namespace}") final String namespace) {
        return new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
    }

}
~~~

#### 2.4 注册SpringJobScheduler 调度程序 

~~~java
package com.willow.config;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class StockJobConfig {

    @Autowired
    private JobRegistryCenterConfig jobRegistryCenterConfig;
    @Autowired
    private ZookeeperRegistryCenter regCenter;

    public StockJobConfig() {
    }




    @Bean(initMethod = "init")
    public JobScheduler simpleJobScheduler(final StockSimpleJob simpleJob, @Value("${stockJob.cron}") final String cron, @Value("${stockJob.shardingTotalCount}") final int shardingTotalCount,
                                           @Value("${stockJob.shardingItemParameters}") final String shardingItemParameters) {
        return new SpringJobScheduler(simpleJob, regCenter, simpleJobConfigBuilder(simpleJob.getClass(), cron, shardingTotalCount, shardingItemParameters));
    }

    /**
     *@Description  任务配置类
     */
    private LiteJobConfiguration simpleJobConfigBuilder(final Class<? extends SimpleJob> jobClass,
                                                         final String cron,
                                                         final int shardingTotalCount,
                                                         final String shardingItemParameters){


        return LiteJobConfiguration
                .newBuilder(
                        new SimpleJobConfiguration(
                                JobCoreConfiguration.newBuilder(
                                        "my-jobName",cron,shardingTotalCount)
                                        .shardingItemParameters(shardingItemParameters).jobParameter("job-参数")
                                        .build()
                                ,jobClass.getCanonicalName()
                        )
                )
                .overwrite(true)
                .build();

    }


    /**
     * 动态添加
     * @param jobClass
     * @param cron
     * @param shardingTotalCount
     * @param shardingItemParameters
     */
    public void addSimpleJobScheduler(final Class<? extends SimpleJob> jobClass,
                       final String cron,
                       final int shardingTotalCount,
                       final String shardingItemParameters){
        JobCoreConfiguration coreConfig = JobCoreConfiguration.newBuilder(jobClass.getName(), cron, shardingTotalCount).shardingItemParameters(shardingItemParameters).build();
        SimpleJobConfiguration simpleJobConfig = new SimpleJobConfiguration(coreConfig, jobClass.getCanonicalName());
        JobScheduler jobScheduler = new JobScheduler(regCenter, LiteJobConfiguration.newBuilder(simpleJobConfig).build());
        jobScheduler.init();
    }

}
~~~

#### 2.5 添加业务逻辑处理

~~~java
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import org.springframework.stereotype.Component;

@Component
public class StockSimpleJob implements SimpleJob {

    @Override
    public void execute(ShardingContext shardingContext) {
        System.out.println(String.format("------Thread ID: %s, 任务总片数: %s, " +
                                         "当前分片项: %s.当前参数: %s,"+
                                         "当前任务名称: %s.当前任务参数: %s"
                                         ,
                                         Thread.currentThread().getId(),
                                         shardingContext.getShardingTotalCount(),
                                         shardingContext.getShardingItem(),
                                         shardingContext.getShardingParameter(),
                                         shardingContext.getJobName(),
                                         shardingContext.getJobParameter()

                                        ));

    }
}
~~~



