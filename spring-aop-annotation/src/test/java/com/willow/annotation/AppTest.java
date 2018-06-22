package com.willow.annotation;

import com.willow.aop.AopConfig;
import com.willow.aop.Calculate;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private Logger logger=LoggerFactory.getLogger(AppTest.class);

    @Test
    public void test(){
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(AopConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();

        for (String beanName:beanDefinitionNames){
            logger.info("############"+beanName);
        }
        Calculate calculate=  applicationContext.getBean(Calculate.class);
        calculate.add(1,2);
    }
}
