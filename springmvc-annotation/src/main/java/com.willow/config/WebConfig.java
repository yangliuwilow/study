package com.willow.config;

import com.willow.web.MyInterceptor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.*;

//SpringMVC只扫描Controller；子容器   ,web容器
//useDefaultFilters=false 禁用默认的过滤规则；
//web容器
@ComponentScan(value = "com.willow",includeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class})},useDefaultFilters = false)
@EnableWebMvc  //相当于<!--  <mvc:annotation-driven />
public class WebConfig extends WebMvcConfigurerAdapter  {     //或者实现 WebMvcConfigurer

  //文档地址 ： https://docs.spring.io/spring/docs/5.0.2.RELEASE/spring-framework-reference/web.html#mvc-config-customize
  //章节：1.11.2. MVC Config API


    //将SpringMVC处理不了的请求交给tomcat；静态资源 就可以访问
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();          //相当于xml文件配置<mvc:default-servlet-handler/>
    }
    //视图解析器
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        // TODO Auto-generated method stub
        //默认所有的页面都从 /WEB-INF/ xxx .jsp
        //registry.jsp();
        registry.jsp("/WEB-INF/views/", ".jsp");
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //  /** 任意存路径
        registry.addInterceptor(new MyInterceptor()).addPathPatterns("/**");
    }

}
