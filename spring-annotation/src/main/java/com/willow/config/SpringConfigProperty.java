package com.willow.config;

import com.willow.bean.Person;
import com.willow.dao.PersionDao;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

@PropertySource(value={"person.properties"})
@Configuration
@ComponentScan({"com.willow"})
public class SpringConfigProperty {

    @Bean
    public Person person2(){
        return new Person();
    }
    @Primary
    @Bean
    public PersionDao persionDao(){
        return new PersionDao();
    }

}
