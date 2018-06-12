SpringBoot  -dubbo 配置

文档地址：https://github.com/apache/incubator-dubbo-spring-boot-project/blob/master/README_CN.md



1.添加pom依赖

```xml
<dependency>
    <groupId>com.alibaba.boot</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>

<!--引入zookeeper的客户端工具--> 
<dependency>
    <groupId>com.github.sgroschupf</groupId>
    <artifactId>zkclient</artifactId>
    <version>0.1</version>
</dependency>
```