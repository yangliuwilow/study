package com.willow.ext;


import com.willow.bean.Color;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;







@ComponentScan("com.willow.ext")
@Configuration
public class ExtConfig {

    @Bean
    public Color color(){
        return  new Color();
    }
}
