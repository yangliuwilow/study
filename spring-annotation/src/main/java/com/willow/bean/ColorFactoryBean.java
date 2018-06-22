package com.willow.bean;

import org.springframework.beans.factory.FactoryBean;

//创建一个Spring定义的FactoryBean
public class ColorFactoryBean implements FactoryBean {
    @Override
    public Object getObject() throws Exception {
        return new Color();
    }

    @Override
    public Class<?> getObjectType() {
        return Color.class;
    }


    //是否单例
    //true  这个bean 是单实例，在容器中保存一份
    //false  多实例的，
    @Override
    public boolean isSingleton() {
        return false;
    }
}
