package com.willow.aop;

import org.springframework.stereotype.Component;

/**
 * Created by Administrator on 2018/6/16.
 */
@Component
public class Calculate {

    public Integer  add(Integer a,Integer b){
        System.out.println("Calculate add...");
          return a+b;
    }
}
