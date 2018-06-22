package com.willow.annotation;

import com.willow.bean.Person;
import com.willow.config.SpringConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MainText {
    public static void main(String[] args) {
        /**
         * 配置的方式获取
         */
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-common.xml");
        Object persion = applicationContext.getBean("persion");
        System.out.println("#############xml-bean:"+persion);

        /**
         * 注解的方式
         */
        ApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        Person bean = annotationConfigApplicationContext.getBean(Person.class);
        System.out.println("#############annotation-bean"+bean);

        /**
         * 按照类型查询beanNames
         */
        String[] beanNames=  annotationConfigApplicationContext.getBeanNamesForType(Person.class);
        for (String beanName:beanNames){
            System.out.println("########Annotation_bean_name:"+beanName);
        }
        /**
         * 获取容器中所有的bean
         */
        String[] beanDefinitionNames = annotationConfigApplicationContext.getBeanDefinitionNames();
        for (String beanName:beanDefinitionNames){
            System.out.println("########beanDefinitionNames_Annotation_bean_name:"+beanName);
        }
    }
}
