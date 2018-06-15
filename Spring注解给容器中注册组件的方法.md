Spring注解详解包扫描&bean注册

### 一. @Configuration 配置

​                 告诉Spring容器这是一个配置类  ==xml配置

### 二.  @ComponentScan 包扫描

​               说明：

~~~java
//value:指定要扫描的包
//按照规则指定需要关闭默认规则，设置 useDefaultFilters=false;
//excludeFilters = Filter[] ：指定扫描的时候按照什么规则排除那些组件
//includeFilters = Filter[] ：指定扫描的时候只需要包含哪些组件
        //FilterType.ANNOTATION：按照注解
        //FilterType.ASSIGNABLE_TYPE：按照给定的类型；
        //FilterType.ASPECTJ：使用ASPECTJ表达式
        //FilterType.REGEX：使用正则指定
        //FilterType.CUSTOM：使用自定义规则
~~~

​          使用例子在配置类上添加：

~~~java

@ComponentScan(value = {"com.willow"},
   includeFilters = {
   	@ComponentScan.Filter(type=FilterType.ANNOTATION,classes={Controller.class}),                  //添加   RController 注解的组件
	@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes={persion.class}),  
     //添加 类型为persion 注解的组件
     @ComponentScan.Filter(type = FilterType.CUSTOM, classes = {MyTypeFilter.class}),  
    // 自定义注解的规则
     @ComponentScan.Filter(type = FilterType.ANNOTATION,classes={Repository.class,Controller.class}) 
   //添加  Repository和，Controller 注解的组件

    }, useDefaultFilters = false  //includeFilters 设置 useDefaultFilters=false;  excludeFilters 设置 useDefaultFilters=true
)   //包扫描
~~~

​       自定义规则扫描 MyTypeFilter：

~~~java
package com.willow.config;


import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

public class MyTypeFilter implements TypeFilter {

    /**
     * metadataReader：读取到的当前正在扫描的类的信息
     * metadataReaderFactory:可以获取到其他任何类信息的
     */
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        //获取当前类注解的信息
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
        //获取当前正在扫描的类的类信息
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        //获取当前类资源（类的路径）
        Resource resource = metadataReader.getResource();

        String className = classMetadata.getClassName();
        System.out.println("--->"+className);
        if(className.contains("er")){  //类名称包含“er”的 返回true,注册到IOC容器中
            return true; 
        }
        return false;   //不注册
    }
}
~~~

### 三.  @Conditional 注解

   在类上添加：满足当前条件，这个类中配置的所有bean注册才能生效； 

   方法上添加：满足当前条件，这个方法的bean注册才能生效； 

```java
@Conditional({WindowsCondition.class})
```

自定义condition 实现接口

~~~java
package com.willow.condition;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class WindowsCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // TODO是否Windows系统
        //1、能获取到ioc使用的beanfactory
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        //2、获取类加载器
        ClassLoader classLoader = context.getClassLoader();
        //3、获取当前环境信息
        Environment environment = context.getEnvironment();
        //4、获取到bean定义的注册类
        BeanDefinitionRegistry registry = context.getRegistry();

        ResourceLoader resourceLoader = context.getResourceLoader();
        
        Class<? extends ConditionContext> aClass = context.getClass();
        
        String property = environment.getProperty("os.name");

        //可以判断容器中的bean注册情况，也可以给容器中注册bean
        boolean definition = registry.containsBeanDefinition("person");
        if(property.contains("Windows")){ //如果是Windows 系统，这个组件才注册
            return true;
        }

        return false;
    }
}
~~~



### 四、给容器中注册组件方法

   

~~~java
 
    /**
      给容器中注册组件方法:
       1）、包扫描+组件标注注解（@Controller/@Service/@Repository/@Component）[自己写的类]
       2）、@Bean[导入的第三方包里面的组件]
       3）、@Import[快速给容器中导入一个组件]
       		1）、@Import(要导入到容器中的组件)；容器中就会自动注册这个组件，id默认是全类名
       		2）、ImportSelector:返回需要导入的组件的全类名数组；
       		3）、ImportBeanDefinitionRegistrar:手动注册bean到容器中
       4）、使用Spring提供的 FactoryBean（工厂Bean）;
       		1）、默认获取到的是工厂bean调用getObject创建的对象
            	Object bean2 = applicationContext.getBean("colorFactoryBean");
                 得到的bean 是colorFactoryBean.getObject()方法中创建的对象
      		2）、要获取工厂Bean本身，我们需要给id前面加一个&
       			&colorFactoryBean
      		例子：Object bean4 = applicationContext.getBean("&colorFactoryBean");
       		System.out.println(bean4.getClass());
     */
      
~~~

####  4.1 包扫描+组件标注注解

​          在类上添加：@Controller/@Service/@Repository/@Component

####  4.2 @Bean 注解   在容器中注册一个bean

~~~java
/*
    * Scope
    * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE    prototype
	* @see ConfigurableBeanFactory#SCOPE_SINGLETON  singleton
	* @see org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST   web环境下  同一个reqeust 一个实例
	* @see org.springframework.web.context.WebApplicationContext#SCOPE_SESSION   web环境下  同一个session 一个实例
	*  prototype：多实例的：ioc容器启动并不会去调用方法创建对象放在容器中。
    * 					每次获取的时候才会调用方法创建对象；
    * singleton：单实例的（默认值）：ioc容器启动会调用方法创建对象放到ioc容器中。
    * 			 以后每次获取就是直接从容器（map.get()）中拿，
    * request：同一次请求创建一个实例
    * session：同一个session创建一个实例
    *
    * 懒加载：  @Lazy
    * 		单实例bean：默认在容器启动的时候创建对象；
    * 		懒加载：容器启动不创建对象。第一次使用(获取)Bean创建对象，并初始化；
	* */
    //默认是单实例的
    //@Scope("prototype")
    @Lazy
    @Bean //(name = "myPersion")  //创建一个bean ,name指定bean的Id，默认Id为方法名称
    public Persion persion() {
        System.out.println("给容器中添加Person....");
        return new Persion(1, "willow", "28");
    }

~~~

#### 4.3 配置类上@Import  

#####          4.3.1  @Import导入指定的类

```java
@Import({Color.class}) //导入组件，id默认是组件的全类

```
#####   4.3.2  实现ImportSelector接口

```java
@Import({Color.class,MyImportSelector.class,MyImportBeanDefinitionRegistrar.class}) //导入组件，id默认是组件的全类名
```
  接口定义：

~~~java

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

//自定义逻辑返回需要导入的组件
public class MyImportSelector implements ImportSelector {

    //返回值，就是到导入到容器中的组件全类名
    //AnnotationMetadata:当前标注@Import注解的类的所有注解信息
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        //方法不要返回null值
       //  return null;
        return new String[]{"com.willow.bean.Red"};
    }
}

~~~

#####   4.3.3  实现ImportBeanDefinitionRegistrar接口 ,手动添加到容器中去

~~~java
import com.willow.bean.RainBow;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    /**
     * AnnotationMetadata：当前类的注解信息
     * BeanDefinitionRegistry:BeanDefinition注册类；
     * 		把所有需要添加到容器中的bean；调用
     * 		BeanDefinitionRegistry.registerBeanDefinition手工注册进来
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        boolean definition = registry.containsBeanDefinition("com.willow.bean.Red");
        boolean definition2 = registry.containsBeanDefinition("com.willow.bean.Blue");
        if(definition ){
            //指定Bean定义信息；（Bean的类型，Bean。。。）
            RootBeanDefinition beanDefinition = new RootBeanDefinition(RainBow.class);
            //注册一个Bean，指定bean名
            registry.registerBeanDefinition("rainBow", beanDefinition);
        }
    }
}
~~~

##### 4.4  使用Spring提供的 FactoryBean（工厂Bean） 

1.实现FactoryBean 接口

~~~java

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
~~~

2.注册 ColorFactoryBean 组件

```java
@Bean
public ColorFactoryBean colorFactoryBean(){
    return new ColorFactoryBean();
}
```

### 五、配置类：

~~~java
package com.willow.config;


import com.willow.bean.Color;
import com.willow.bean.ColorFactoryBean;
import com.willow.bean.Persion;
import com.willow.bean.Red;
import com.willow.condition.LiunxCondition;
import com.willow.condition.MyImportBeanDefinitionRegistrar;
import com.willow.condition.MyImportSelector;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;


//类中组件统一设置。满足当前条件，这个类中配置的所有bean注册才能生效；
//@Conditional({LiunxCondition.class})
@Configuration  //告诉Spring容器这是一个配置类
@ComponentScan(value = {"com.willow"},
        includeFilters = {
         /*		@ComponentScan.Filter(type=FilterType.ANNOTATION,classes={Controller.class}), //添加   RController 注解的组件
				@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes={persion.class}), */    //添加 类型为persion 注解的组件
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = {MyTypeFilter.class})  // 自定义注解的规则
                //  , @ComponentScan.Filter(type = FilterType.ANNOTATION,classes={Repository.class,Controller.class})  //添加  Repository和，Controller 注解的组件

        }, useDefaultFilters = false  //includeFilters 设置 useDefaultFilters=false;  excludeFilters 设置 useDefaultFilters=true
)   //包扫描
//@ComponentScan  value:指定要扫描的包

//按照规则指定需要关闭默认规则，设置 useDefaultFilters=false;
//excludeFilters = Filter[] ：指定扫描的时候按照什么规则排除那些组件
//includeFilters = Filter[] ：指定扫描的时候只需要包含哪些组件
//FilterType.ANNOTATION：按照注解
//FilterType.ASSIGNABLE_TYPE：按照给定的类型；
//FilterType.ASPECTJ：使用ASPECTJ表达式
//FilterType.REGEX：使用正则指定
//FilterType.CUSTOM：使用自定义规则
@Import({Color.class,MyImportSelector.class,MyImportBeanDefinitionRegistrar.class}) //导入组件，id默认是组件的全类名
public class SpringConfig {

   /*
    * Scope
    * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE    prototype
	* @see ConfigurableBeanFactory#SCOPE_SINGLETON  singleton
	* @see org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST   web环境下  同一个reqeust 一个实例
	* @see org.springframework.web.context.WebApplicationContext#SCOPE_SESSION   web环境下  同一个session 一个实例
	*  prototype：多实例的：ioc容器启动并不会去调用方法创建对象放在容器中。
    * 					每次获取的时候才会调用方法创建对象；
    * singleton：单实例的（默认值）：ioc容器启动会调用方法创建对象放到ioc容器中。
    * 			 以后每次获取就是直接从容器（map.get()）中拿，
    * request：同一次请求创建一个实例
    * session：同一个session创建一个实例
    *
    * 懒加载：  @Lazy
    * 		单实例bean：默认在容器启动的时候创建对象；
    * 		懒加载：容器启动不创建对象。第一次使用(获取)Bean创建对象，并初始化；
	* */
    //默认是单实例的
    //@Scope("prototype")
    @Lazy
    @Bean //(name = "myPersion")  //创建一个bean ,name指定bean的Id，默认Id为方法名称
    public Persion persion() {
        System.out.println("给容器中添加Person....");
        return new Persion(1, "willow", "28");
    }

    /**
     * @Conditional({Condition}) ： 按照一定的条件进行判断，满足条件给容器中注册bean
     * @return
     */
    @Conditional(LiunxCondition.class)
    @Bean("myPersion")  //创建一个bean ,name指定bean的Id，默认Id为方法名称
    public Persion liunx() {
        System.out.println("给容器中添加Person....");
        return new Persion(1, "liunx", "58");
    }



    /**
     * 给容器中注册组件方法:
     * 1）、包扫描+组件标注注解（@Controller/@Service/@Repository/@Component）[自己写的类]
     * 2）、@Bean[导入的第三方包里面的组件]
     * 3）、@Import[快速给容器中导入一个组件]
     * 		1）、@Import(要导入到容器中的组件)；容器中就会自动注册这个组件，id默认是全类名
     * 		2）、ImportSelector:返回需要导入的组件的全类名数组；
     * 		3）、ImportBeanDefinitionRegistrar:手动注册bean到容器中
     * 4）、使用Spring提供的 FactoryBean（工厂Bean）;
     * 		1）、默认获取到的是工厂bean调用getObject创建的对象
     *      	Object bean2 = applicationContext.getBean("colorFactoryBean");
     *           得到的bean 是colorFactoryBean.getObject()方法中创建的对象
     * 		2）、要获取工厂Bean本身，我们需要给id前面加一个&
     * 			&colorFactoryBean
     * 		例子：Object bean4 = applicationContext.getBean("&colorFactoryBean");
     * 		System.out.println(bean4.getClass());
     */
    @Bean
    public ColorFactoryBean colorFactoryBean(){
        return new ColorFactoryBean();
    }

}

~~~

六、测试类：

~~~java
import com.willow.bean.Persion;
import com.willow.config.SpringConfig;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class AppTest {

    /**
     * 注解的方式
     */
    ApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(SpringConfig.class);
    @Test
    public void test03() {
        /**
         * 获取容器中所有的bean
         */
        String[] beanDefinitionNames = annotationConfigApplicationContext.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            System.out.println("########beanDefinitionNames_Annotation_bean_name:" + beanName);
        }
        Persion persion= annotationConfigApplicationContext.getBean(Persion.class);
        Persion persion01=  annotationConfigApplicationContext.getBean(Persion.class);
        System.out.println("判断bean相等:"+persion==persion01+"");   // @Scope("prototype") 不相等 ，单例：singleton 时候相等
    }

   
}
~~~

