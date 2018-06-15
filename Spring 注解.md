

Spring 注解驱动学习视频：

https://www.bilibili.com/video/av20967380?from=search&seid=5128648131851822889

Configuration说明  

```java
@Configuration  //告诉Spring容器这是一个配置类
public class SpringConfig {
    @Bean(name="myPersion")  //创建一个bean ,name指定bean的Id，默认Id为方法名称
    public Persion  persion(){
        return new Persion(1,"willow","28");
    }
}
 
```

xml配置：

```xml
<bean id="persion" class="com.willow.bean.Persion" lazy-init="true">
    <property name="age" value="12"></property>
    <property name="id" value="1"></property>
    <property name="name" value="yangliu"></property>
</bean>
```