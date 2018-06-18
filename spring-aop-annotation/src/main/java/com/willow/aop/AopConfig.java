package com.willow.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Created by Administrator on 2018/6/16.
 */
@Configuration
@ComponentScan("com.willow.aop")
@EnableAspectJAutoProxy   //开启注解的AOP模式
public class AopConfig {
    @Bean
    public LogAspects LogAspects(){   //注册到IOC容器中
        return  new LogAspects();
    }
}
