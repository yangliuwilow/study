package com.willow.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class Color  implements InitializingBean,DisposableBean {
    @Override
    public void destroy() throws Exception {
        System.out.println("color ....disposableBean");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("color ....afterPropertiesSet");
    }
}
