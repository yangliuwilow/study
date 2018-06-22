package com.willow.annotation;


import com.willow.bean.Person;
import com.willow.config.SpringConfigLife;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class AppTestLife {

    /**
     * 注解的方式
     */

    @Test
    public void test03() {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(SpringConfigLife.class);
        /**
         * 获取容器中所有的bean
         */
        String[] beanDefinitionNames = annotationConfigApplicationContext.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            System.out.println("########beanDefinitionNames_Annotation_bean_name:" + beanName);
        }
        Person person = annotationConfigApplicationContext.getBean(Person.class);
        Person person01 =  annotationConfigApplicationContext.getBean(Person.class);
        System.out.println("判断bean相等:"+ person == person01 +"");   // @Scope("prototype") 不相等 ，单例：singleton 时候相等
        annotationConfigApplicationContext.close();
    }


}
