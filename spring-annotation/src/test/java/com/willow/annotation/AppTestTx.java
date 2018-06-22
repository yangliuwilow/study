package com.willow.annotation;


import com.willow.tx.DeptService;
import com.willow.tx.TxConfig;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class AppTestTx {

    /**
     * 注解的方式
     */
    AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(TxConfig.class);
    @Test
    public void test03() {
        /**
         * 获取容器中所有的bean
         */

        DeptService deptService = annotationConfigApplicationContext.getBean(DeptService.class);
        deptService.add();
        annotationConfigApplicationContext.close();
    }


}
