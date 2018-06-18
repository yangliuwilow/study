package com.willow.annotation;

import com.willow.aop.AopConfig;
import com.willow.aop.Calculate;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Unit test for simple App.
 */
public class AppTest {


    @Test
    public void test(){
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(AopConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();

        for (String beanName:beanDefinitionNames){
            System.out.println("############"+beanName);
        }
        Calculate calculate=  applicationContext.getBean(Calculate.class);
        calculate.add(1,2);
    }
}
