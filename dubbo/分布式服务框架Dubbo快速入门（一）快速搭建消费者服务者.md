# 分布式服务框架Dubbo快速入门（一）快速搭建XML消费者服务者



官方文档：http://dubbo.apache.org/#/docs/user/quick-start.md?lang=zh-cn

dubbo源码地址：https://github.com/apache/incubator-dubbo/tags

### 1、创建父项目spring-dubbo

####  1.1、父项目pom依赖

~~~xml
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.willow</groupId>
    <artifactId>spring-dubbo</artifactId>
    <packaging>pom</packaging>
    <version>1.0-SNAPSHOT</version>
    <modules>
        <module>spring-dubbo-api</module>
        <module>spring-dubbo-provider</module>
        <module>spring-dubbo-consumer</module>
    </modules>

    <name>spring-dubbo</name>
    
    <url>http://www.example.com</url>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.7</maven.compiler.source>
        <maven.compiler.target>1.7</maven.compiler.target>

        <dubbo.version>2.6.0</dubbo.version>
        <zookeeper.version>3.4.9</zookeeper.version>
        <zkclient.version>0.2</zkclient.version>
        <spring.version>4.3.10.RELEASE</spring.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>dubbo</artifactId>
            <version>${dubbo.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <version>${zookeeper.version}</version>
        </dependency>
        <dependency>
            <groupId>com.101tec</groupId>
            <artifactId>zkclient</artifactId>
            <version>${zkclient.version}</version>
        </dependency>
    </dependencies>
</project>

~~~

### 2、创建子项目spring-dubbo-api

#### 2.1 创建UserService接口

~~~java
package com.willow.service;

import com.willow.entity.User;

import java.util.List;


public interface UserService   {

      List<User> selectList(User user);
}
~~~

### 3、创建子项目服务提供者spring-dubbo-provider

####    3.1 服务提供者POM依赖

~~~xml
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>spring-dubbo</artifactId>
        <groupId>com.willow</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>spring-dubbo-provider</artifactId>

    <name>spring-dubbo-provider</name>
    <url>http://www.example.com</url>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.7</maven.compiler.source>
        <maven.compiler.target>1.7</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- 添加API依赖-->
        <dependency>
            <groupId>com.willow</groupId>
            <artifactId>spring-dubbo-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

    </dependencies>

</project>

~~~

#### 3.2 创建UserImpl 实现类

~~~java
package com.willow.service.impl;

import com.willow.entity.User;
import com.willow.service.UserService;

import java.util.ArrayList;
import java.util.List;

public class UserImpl implements UserService {
    @Override
    public List<User> selectList(User user) {
        List<User> list=new ArrayList<User>();
        list.add(new User(1,"zhangsan",29));
        list.add(new User(2,"lisi",19));
        return list;
    }
}
~~~

#### 3.3  resources 目录下创建dubbo提供者的配置文件spring-provider.xml

参考：https://github.com/apache/incubator-dubbo/tree/master/dubbo-demo/dubbo-demo-provider/src/main/resources

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://code.alibabatech.com/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
       http://code.alibabatech.com/schema/dubbo http://code.alibabatech.com/schema/dubbo/dubbo.xsd">


    <!-- 提供方应用名称，用于计算依赖关系 -->
    <dubbo:application name="spring-dubbo-provider"/>

    <!-- 使用multicast注册中心暴露服务地址   默认为2181端口 -->
    <dubbo:registry address="zookeeper://192.168.7.108:2181"/>

    <!-- 使用dubbo协议，在20880端口暴露服务 -->
    <dubbo:protocol name="dubbo" port="20880"/>

    <bean id="userService" class="com.willow.service.impl.UserImpl"/>

    <dubbo:service interface="com.willow.service.UserService" ref="userService"/>
</beans>
~~~

#### 3.4 服务提供者启动类

~~~java
package com.willow;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * SpringDubboProvider
 *
 */
public class SpringDubboProvider
{
    public static void main( String[] args )
    {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring-provider.xml"});
        context.start();
        while (true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

~~~

### 4、创建子项目服务消费者spring-dubbo-consumer

####    4.1 服务提供者POM依赖

~~~xml
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>spring-dubbo</artifactId>
        <groupId>com.willow</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>spring-dubbo-consumer</artifactId>
    <packaging>war</packaging>

    <name>spring-dubbo-consumer Maven Webapp</name>
    <url>http://www.example.com</url>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.7</maven.compiler.source>
        <maven.compiler.target>1.7</maven.compiler.target>
    </properties>

    <dependencies>

        <dependency>
            <groupId>com.willow</groupId>
            <artifactId>spring-dubbo-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

    </dependencies>
    
</project>

~~~

#### 4.2 resources 目录下创建dubbo消费者的配置文件spring-consumer.xml

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://code.alibabatech.com/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
       http://code.alibabatech.com/schema/dubbo http://code.alibabatech.com/schema/dubbo/dubbo.xsd">



    <!-- 消费方应用名，用于计算依赖关系，不是匹配条件，不要与提供方一样 -->
    <dubbo:application name="spring-dubbo-consumer"  />

    <!-- 使用zookeeper广播注册中心暴露发现服务地址 -->
    <dubbo:registry address="zookeeper://192.168.7.108:2181" />

    <!-- 生成远程服务代理，可以和本地bean一样使用demoService -->
    <dubbo:reference id="userService" interface="com.willow.service.UserService" />
</beans>
~~~

#### 4.3  创建服务调用方法

~~~java
package com.willow;

import com.willow.entity.User;
import com.willow.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class SpringDubboConsumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring-consumer.xml"});
        context.start();
        UserService userService = (UserService)context.getBean("userService"); // 获取远程服务代理
        List<User> hello = userService.selectList(new User()); // 执行远程方法
        System.out.println( hello ); // 显示调用结果
    }
}

~~~



代码地址：https://github.com/yangliuwilow/dubbo-projects/tree/master/spring-dubbo

### 5、xml属性配置解释



| 标签                   | 用途         | 解释                                                         |
| ---------------------- | ------------ | ------------------------------------------------------------ |
| `<dubbo:service/>`     | 服务配置     | 用于暴露一个服务，定义服务的元信息，一个服务可以用多个协议暴露，一个服务也可以注册到多个注册中心 |
| `<dubbo:reference/>`   | 引用配置     | 用于创建一个远程服务代理，一个引用可以指向多个注册中心       |
| `<dubbo:protocol/>`    | 协议配置     | 用于配置提供服务的协议信息，协议由提供方指定，消费方被动接受 |
| `<dubbo:application/>` | 应用配置     | 用于配置当前应用信息，不管该应用是提供者还是消费者           |
| `<dubbo:module/>`      | 模块配置     | 用于配置当前模块信息，可选                                   |
| `<dubbo:registry/>`    | 注册中心配置 | 用于配置连接注册中心相关信息                                 |
| `<dubbo:monitor/>`     | 监控中心配置 | 用于配置连接监控中心相关信息，可选                           |
| `<dubbo:provider/>`    | 提供方配置   | 当 ProtocolConfig 和 ServiceConfig 某属性没有配置时，采用此缺省值，可选 |
| `<dubbo:consumer/>`    | 消费方配置   | 当 ReferenceConfig 某属性没有配置时，采用此缺省值，可选      |
| `<dubbo:method/>`      | 方法配置     | 用于 ServiceConfig 和 ReferenceConfig 指定方法级的配置信息   |
| `<dubbo:argument/>`    | 参数配置     | 用于指定方法参数配置                                         |