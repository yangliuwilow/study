#        SpringMvc-annotation  原理： 

 文档地址：https://docs.spring.io/spring/docs/5.0.2.RELEASE/spring-framework-reference/web.html#mvc-introduction

创建项目导入POM依赖

~~~xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.atguigu</groupId>
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

   会扫描每个jar包下的META-INF/services/javax.servlet.ServletContainerInitializer

   在jarMaven: org.springframework:spring-web:4.3.11.RELEASE   有对应的文件，

​    中指定了org.springframework.web.SpringServletContainerInitializer这个类

### 2、加载这个文件指定的类（SpringServletContainerInitializer）

### 3、spring的应用一启动会加载感兴趣的WebApplicationInitializer接口的下的所有组件；

```java
@HandlesTypes(WebApplicationInitializer.class)//加载这个接口的实现类，抽象类，extends
```

### 4、并且为WebApplicationInitializer组件创建对象

   前提：（这些组件不是接口，不是抽象类才创建对象）这个类的三个子组件

- AbstractContextLoaderInitializer (org.springframework.web.context) 
- AbstractDispatcherServletInitializer (org.springframework.web.servlet.support) 
- AbstractAnnotationConfigDispatcherServletInitializer (org.springframework.web.servlet.support) 

 

####    4.1 AbstractContextLoaderInitializer 

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



```java
//重写了AbstractDispatcherServletInitializer 的方法
protected WebApplicationContext createServletApplicationContext() {
    AnnotationConfigWebApplicationContext servletAppContext = new AnnotationConfigWebApplicationContext();// //创建web的ioc容器
    Class<?>[] configClasses = getServletConfigClasses(); //获取配置类
    if (!ObjectUtils.isEmpty(configClasses)) {
        servletAppContext.register(configClasses);
    }
    return servletAppContext;
}
```