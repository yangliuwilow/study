package com.willow.service;

import com.willow.dao.PersionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PersionService {

    @Qualifier("persionDao")   //使用@Qualifier指定需要装配的组件的id，而不是使用属性名
    @Autowired(required = false)  //有persionDao这个bean就注解没有不报错
    public PersionDao persionDao;


    @Qualifier("persionDao2")   //使用@Qualifier指定需要装配的组件的id，而不是使用属性名
    @Autowired(required = false)  //有persionDao这个bean就注解没有不报错
    public PersionDao persionDao2;
}
