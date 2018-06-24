package com.willow.service;

import org.springframework.stereotype.Service;

/**
 * Created by Administrator on 2018/6/24.
 */
@Service
public class HelloService {
    public String sayHello(String name){

        return "Hello "+name;
    }
}
