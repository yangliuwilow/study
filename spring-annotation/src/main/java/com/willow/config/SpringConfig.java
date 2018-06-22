package com.willow.config;


import com.willow.bean.Color;
import com.willow.bean.ColorFactoryBean;
import com.willow.bean.Person;
import com.willow.condition.LiunxCondition;
import com.willow.condition.MyImportBeanDefinitionRegistrar;
import com.willow.condition.MyImportSelector;
import org.springframework.context.annotation.*;


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
    public Person persion() {
        System.out.println("给容器中添加Person....");
        return new Person(1, "willow", "28");
    }

    /**
     * @Conditional({Condition}) ： 按照一定的条件进行判断，满足条件给容器中注册bean
     * @return
     */
    @Conditional(LiunxCondition.class)
    @Bean("myPersion")  //创建一个bean ,name指定bean的Id，默认Id为方法名称
    public Person liunx() {
        System.out.println("给容器中添加Person....");
        return new Person(1, "liunx", "58");
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
