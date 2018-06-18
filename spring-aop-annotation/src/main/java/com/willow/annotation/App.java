package com.willow.annotation;

import com.willow.aop.AopConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(AopConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();

        for (String beanName:beanDefinitionNames){
            System.out.println("############"+beanName);
        }
    }
}
