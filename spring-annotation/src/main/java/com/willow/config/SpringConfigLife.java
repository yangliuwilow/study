package com.willow.config;

import com.willow.bean.Color;
import com.willow.bean.MyBeanPostProcessor;
import com.willow.bean.Person;
import com.willow.bean.Red;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MyBeanPostProcessor.class,Red.class})
public class SpringConfigLife {

    //@Lazy
    @Bean(initMethod="init",destroyMethod = "destory")
    public Person persion() {
        System.out.println("给容器中添加Person....");
        return new Person(1, "willow", "28");
    }
    @Bean
    public Color color() {
        System.out.println("给容器中添加color....");
        return new Color();
    }

}
