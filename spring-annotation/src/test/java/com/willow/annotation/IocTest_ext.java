package com.willow.annotation;


import com.willow.bean.Person;
import com.willow.config.SpringConfig;
import com.willow.ext.ExtConfig;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class IocTest_ext {

    /**
     * 注解的方式
     */

    @Test
    public void test01() {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(ExtConfig.class);
        ApplicationEvent applicationEvent=new ApplicationEvent(new String("我发布的一个事件")){};
        annotationConfigApplicationContext.publishEvent(applicationEvent );
        annotationConfigApplicationContext.close();
    }


}
