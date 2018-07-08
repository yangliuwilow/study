# SpringMVC学习笔记（一）ServletContainerInitializer与Spring MVC加载原理：

 文档地址：https://docs.spring.io/spring/docs/5.0.2.RELEASE/spring-framework-reference/web.html#mvc-introduction

创建项目导入POM依赖

~~~xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.willow</groupId>
    <artifactId>springmvc-annotation</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>war</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>4.3.11.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <version>3.0-alpha-1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
             <!--  web项目去掉web.xml的验证功能-->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>2.4</version>
                <configuration>
                    <failOnMissingWebXml>false</failOnMissingWebXml>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
~~~



### 1、web容器在启动的时候

​		在web容器启动时为提供给第三方组件机会做一些初始化的工作，例如注册servlet或者filtes等，servlet规范中通过`ServletContainerInitializer`实现此功能。每个框架要使用`ServletContainerInitializer`就必须在对应的jar包的META-INF/services 目录创建一个名为`javax.servlet.ServletContainerInitializer`的文件，文件内容指定具体的`ServletContainerInitializer`实现类，那么，当web容器启动时就会运行这个初始化器做一些组件内的初始化工作。

一般伴随着`ServletContainerInitializer`一起使用的还有`HandlesTypes`注解，通过`HandlesTypes`可以将感兴趣的一些类注入到`ServletContainerInitializerde`的onStartup方法作为参数传入。

Tomcat容器的`ServletContainerInitializer`机制的实现，主要交由Context容器和ContextConfig监听器共同实现，ContextConfig监听器负责在容器启动时读取每个web应用的`WEB-INF/lib`目录下包含的jar包的`META-INF/services/javax.servlet.ServletContainerInitializer`，以及web根目录下的`META-INF/services/javax.servlet.ServletContainerInitializer`，通过反射完成这些`ServletContainerInitializer`的实例化，然后再设置到Context容器中，最后Context容器启动时就会分别调用每个`ServletContainerInitializer`的onStartup方法，并将感兴趣的类作为参数传入



首先通过ContextConfig监听器遍历每个jar包或web根目录的`META-INF/services/javax.servlet.ServletContainerInitializer`文件，根据读到的类路径实例化每个`ServletContainerInitializer`；然后再分别将实例化好的`ServletContainerInitializer`设置进Context容器中；最后Context容器启动时分别调用所有`ServletContainerInitializer`对象的onStartup方法。

假如读出来的内容为`com.seaboat.mytomcat.CustomServletContainerInitializer`，则通过反射实例化一个`CustomServletContainerInitializer`对象，这里涉及到一个`@HandlesTypes`注解的处理，被它标明的类需要作为参数值传入到onStartup方法。 

​         

### 2、加载这个文件指定的类（SpringServletContainerInitializer）

​		在jarMaven: org.springframework:spring-web:4.3.11.RELEASE   有对应的文件，META-INF/services/javax.servlet.ServletContainerInitializer

中指定了org.springframework.web.SpringServletContainerInitializer这个类

### 3、Spring的应用一启动会加载感兴趣的WebApplicationInitializer接口的下的所有组件；

```java
@HandlesTypes(WebApplicationInitializer.class)//加载这个接口的实现类，抽象类，extends
```

### 4、并且为WebApplicationInitializer组件创建对象

   前提：（这些组件不是接口，不是抽象类才创建对象）这个类的三个子组件

- AbstractContextLoaderInitializer (org.springframework.web.context) 
- AbstractDispatcherServletInitializer (org.springframework.web.servlet.support) 
- AbstractAnnotationConfigDispatcherServletInitializer (org.springframework.web.servlet.support) 

 

####    4.1 创建根容器  AbstractContextLoaderInitializer 

~~~java
//AbstractContextLoaderInitializer
protected void registerContextLoaderListener(ServletContext servletContext) {
    //创建根容器
    WebApplicationContext rootAppContext = createRootApplicationContext();
    if (rootAppContext != null) {
        ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
        listener.setContextInitializers(getRootApplicationContextInitializers());
        servletContext.addListener(listener);
    }
    else {
        logger.debug("No ContextLoaderListener registered, as " +
                     "createRootApplicationContext() did not return an application context");
    }
}
~~~



#### 4.2 AbstractDispatcherServletInitializer  配置DispatcherServlet初始化器

~~~java
protected void registerDispatcherServlet(ServletContext servletContext) {
    String servletName = this.getServletName();
    Assert.hasLength(servletName, "getServletName() must not return empty or null");
    //创建一个web的IOC容器
    WebApplicationContext servletAppContext = this.createServletApplicationContext();
    Assert.notNull(servletAppContext, "createServletApplicationContext() did not return an application context for servlet [" + servletName + "]");
    //创建了DispatcherServlet；
    FrameworkServlet dispatcherServlet = this.createDispatcherServlet(servletAppContext);
    dispatcherServlet.setContextInitializers(this.getServletApplicationContextInitializers());
    //将创建的DispatcherServlet添加到ServletContext中；
    Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
    Assert.notNull(registration, "Failed to register servlet with name \'" + servletName + "\'.Check if there is another servlet registered under the same name.");
    registration.setLoadOnStartup(1);
    //DispatcherServlet的映射，
    registration.addMapping(this.getServletMappings());
    registration.setAsyncSupported(this.isAsyncSupported());
    //添加过滤器
    Filter[] filters = this.getServletFilters();
    if(!ObjectUtils.isEmpty(filters)) {
        Filter[] var7 = filters;
        int var8 = filters.length;

        for(int var9 = 0; var9 < var8; ++var9) {
            Filter filter = var7[var9];
            this.registerServletFilter(servletContext, filter);
        }
    }

    this.customizeRegistration(registration);
}
~~~

#### 4.3 AbstractAnnotationConfigDispatcherServletInitializer 注解方式配置的DispatcherServlet初始化器

createRootApplicationContext()

~~~java
protected WebApplicationContext createRootApplicationContext() {
    Class<?>[] configClasses = getRootConfigClasses();//传入一个配置类,这个抽象的，留给我们自己写的
    if (!ObjectUtils.isEmpty(configClasses)) {
        AnnotationConfigWebApplicationContext rootAppContext = new AnnotationConfigWebApplicationContext();
        rootAppContext.register(configClasses); //添加配置类到容器
        return rootAppContext;
    }
    else {
        return null;
    }
}
~~~



~~~java
// 重写了AbstractDispatcherServletInitializer 的方法
protected WebApplicationContext createServletApplicationContext() {
    AnnotationConfigWebApplicationContext servletAppContext = new AnnotationConfigWebApplicationContext();   //创建web的ioc容器
    Class<?>[] configClasses = getServletConfigClasses(); //获取配置类
    if (!ObjectUtils.isEmpty(configClasses)) {
        servletAppContext.register(configClasses);
    }
    return servletAppContext;
}
~~~



### 5.SpringMvc 注解版无xml web应用

DispatcherServlet  VS  ContextLoaderListener

在 Spring MVC 中存在两种应用上下文：`DispatcherServlet` 创建的和拦截器 `ContextLoaderListener` 创建的上下文：

- `DispatcherServlet`：加载包含 web 组件的 bean，比如 controllers，view resolvers 和 hanlder mappings。
- `ContextLoaderListener`：加载其他 bean，通常是一些中间层和数据层的组件（比如数据库配置 bean 等）。

在 `AbstractAnnotationConfigDispatcherServletInitializer` 中 `DispatcherServlet` 和 `ContextLoaderListener` 都会被创建，而基类中的方法就可用来创建不同的应用上下文：

- `getServletConfigClasses()`：定义 `DispatcherServlet` 应用上下文中的 beans
- `getRootConfigClasses()`：定义拦截器 `ContextLoaderListener` 应用上下文中的 beans

**Note：**为了使用 `AbstractAnnotationConfigDispatcherServletInitializer` 必须保证 web 服务器支持 Servlet 3.0 标准（如 tomcat 7 或更高版本） 。



#### 5.1 Spring Web MVC 注解配置

   文档地址：[1.2.1. Context Hierarchy](https://docs.spring.io/spring/docs/5.0.2.RELEASE/spring-framework-reference/web.html#mvc-servlet-context-hierarchy)

```java
import com.willow.config.WebConfig;
import com.willow.config.RootConfig;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Created by Administrator on 2018/6/24.
 */
//web容器启动的时候创建对象；调用方法来初始化容器以前前端控制器
public class MyWebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    //获取根容器的配置类；（Spring的配置文件）   父容器；
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{RootConfig.class};
    }
    //获取web容器的配置类（SpringMVC配置文件）  子容器；
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{WebConfig.class};
    }
    //获取DispatcherServlet的映射信息
    //  /：拦截所有请求（包括静态资源（xx.js,xx.png）），但是不包括*.jsp；
    //  /*：拦截所有请求；连*.jsp页面都拦截；jsp页面是tomcat的jsp引擎解析的；
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}
```



##### 5.1.1 配置父容器，除了扫描Controller，其他都扫描

扫描，service，数据源等组件

~~~java
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * Created by Administrator on 2018/6/24.
 */
//Spring的容器不扫描controller;父容器
@ComponentScan(value = "com.willow",excludeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class})})
public class RootConfig {
}
~~~

##### 5.1.2 配置web容器，只扫描Controller 

  继承WebMvcConfigurerAdapter 或者实现WebMvcConfigurer 接口，重写相应的方法，修改相应的配置

​     Spring 使用如下方法开启 MVC 的支持：

- `@EnableWebMvc` 注解（JavaConfig）：和 `@Configuration` 注解一起使用
- `<mvc:annotation-driven />` 元素（XML 配置）

开启 MVC 支持，它会从 [`WebMvcConfigurationSupport`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/WebMvcConfigurationSupport.html) 导入 Spring MVC 的配置，会在处理请求时加入**注解的支持**（比如 `@RequestMapping`，`@ExceptionHandler`等注解）。

如果需要自定义配置，从 `@EnableWebMvc` 的[文档](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/EnableWebMvc.html)上来看，需要继承 `@WebMvcConfigurer` 接口或者继承基类 `WebMvcConfigurerAdapter`（它继承了 `@WebMvcConfigurer` 接口，但是用空方法实现）。所以，覆盖相应的方法就能实现 mvc 配置的自定义。

~~~java
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


    //注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //  /** 任意存路径
        registry.addInterceptor(new MyInterceptor()).addPathPatterns("/**");
    }

}
~~~

#### 5.2  Spring web MVC xml配置(相当于5.1配置的内容)

```xml
<web-app>

    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/root-context.xml</param-value>
    </context-param>

    <servlet>
        <servlet-name>app1</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/app1-context.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>app1</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

#### 5.3 添加拦截器

```java
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyInterceptor implements HandlerInterceptor {
    //目标方法运行之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        System.out.println("preHandle..."+request.getRequestURI());
        return true;
    }

    //目标方法执行正确以后执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle...");

    }

    //页面响应以后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
        throws Exception {
        System.out.println("afterCompletion...");
    }
}
```

### 6.SpringMVC异步处理请求

Spring MVC 3.2开始引入Servlet 3中的基于异步的处理request.往常是返回一个值,而现在是一个Controller方法可以返回一个`java.util.concurrent.Callable`对象和从Spring MVC的托管线程生产返回值.同时Servlet容器的主线程退出和释放,允许处理其他请求。Spring MVC通过`TaskExecutor`的帮助调用`Callable`在一个单独的线程。并且当这个`Callable`返回时,这个rquest被分配回Servlet容器使用由`Callable`的返回值继续处理。  

~~~java
@Controller
public class AsyncController {


    //异步处理请求方法二，

    /**
     * 第一步：创建一个  DeferredResult<Object> deferredResult
     * 第二步：存储到queue 中，或者存储到mq中
     * 第三步：获取到请求，返回 DeferredResult<Object> deferredResult = DeferredResultQueue.get();
     *         deferredResult.setResult(order); //返回结果，
     * @return
     */

    @ResponseBody
    @RequestMapping("/createOrder")
    public DeferredResult<Object> createOrder(){
        DeferredResult<Object> deferredResult = new DeferredResult<>((long)3000, "create fail...");  //超时时间，超时返回结果
        DeferredResultQueue.save(deferredResult);  //deferredResult  存储到queue
        return deferredResult;
    }


    @ResponseBody
    @RequestMapping("/create")
    public String create(){
        //创建订单
        String order = UUID.randomUUID().toString();
        DeferredResult<Object> deferredResult = DeferredResultQueue.get();
        deferredResult.setResult(order); //返回结果，
        return "success===>"+order;
    }



    /**
     * 1、控制器返回Callable
     * 2、Spring异步处理，将Callable 提交到 TaskExecutor 使用一个隔离的线程进行执行
     * 3、DispatcherServlet和所有的Filter退出web容器的线程，但是response 保持打开状态；
     * 4、Callable返回结果，SpringMVC将请求重新派发给容器，恢复之前的处理；
     * 5、根据Callable返回的结果。SpringMVC继续进行视图渲染流程等（从收请求-视图渲染）。
     *
     * preHandle.../springmvc-annotation/async01
     主线程开始...Thread[http-bio-8081-exec-3,5,main]==>1513932494700
     主线程结束...Thread[http-bio-8081-exec-3,5,main]==>1513932494700
     =========DispatcherServlet及所有的Filter退出线程============================

     ================等待Callable执行==========
     副线程开始...Thread[MvcAsync1,5,main]==>1513932494707
     副线程开始...Thread[MvcAsync1,5,main]==>1513932496708
     ================Callable执行完成==========

     ================再次收到之前重发过来的请求========
     preHandle.../springmvc-annotation/async01
     postHandle...（Callable的之前的返回值就是目标方法的返回值）
     afterCompletion...

     异步的拦截器:
     1）、原生API的AsyncListener
     2）、SpringMVC：实现AsyncHandlerInterceptor；
     * @return
     */
    @ResponseBody
    @RequestMapping("async")
    public Callable<String> async(){
        System.out.println("主线程执行开始："+Thread.currentThread().getName()+"--->"+System.currentTimeMillis());
        Callable<String> callable=new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println("子线程执行开始："+Thread.currentThread().getName()+"--->"+System.currentTimeMillis());
                Thread.sleep(3000);
                System.out.println("子线程执行结束："+Thread.currentThread().getName()+"--->"+System.currentTimeMillis());
                return "async - callable";
            }
        };
        System.out.println("主线程执行结束："+Thread.currentThread().getName()+"--->"+System.currentTimeMillis());
        return  callable;

    }


}
~~~



**代码地址**：https://github.com/yangliuwilow/study/tree/master/springmvc-annotation