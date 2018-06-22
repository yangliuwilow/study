package com.willow.bean;


import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class Red {


    @PostConstruct
    public void init(){
        System.out.println("red.....init.......");
    }

    @PreDestroy()
    public void destory(){
        System.out.println("red.....destory.............");
    }
}
