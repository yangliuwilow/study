Spring 注解加载外部属性和自动装配

### 一、@PropertySource  加载外面资源文件

~~~java
@PropertySource(value={"person.properties"})
@Configuration
public class SpringConfigProperty {

    @Bean
    public Person person(){
        return new Person();
    }

~~~
xml 加载外部资源方式
 

```xml
<context:property-placeholder  location="classpath:spring-common.xml"/>
```



### 二、@value 赋值的使用

~~~java
//使用@Value赋值；
//1、基本数值
//2、可以写SpEL； #{}
//3、可以写${}；取出配置文件【properties】中的值（在运行环境变量里面的值）
    @Value("1")
    private Integer id;
    @Value("${person.nickName}")
    private String name;
    @Value("#{20-2}")
    private String age;

~~~

### 三、IOC容器获取配置文件信息

~~~java
@Test
public void test03() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfigProperty.class);
        /**
         * 获取容器中所有的bean
         */
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            System.out.println("########beanDefinitionNames_Annotation_bean_name:" + beanName);
        }

        Person person = (Person) applicationContext.getBean("person");
        System.out.println(person);


        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String property = environment.getProperty("person.nickName");
        System.out.println(property);

        applicationContext.close();
}

~~~



### 四、bean自动装备：

```java
/**
 * 自动装配;
 *        Spring利用依赖注入（DI），完成对IOC容器中中各个组件的依赖关系赋值；
 * 
 * 1）、@Autowired：自动注入：（按类型装配）
 *        1）、默认优先按照类型去容器中找对应的组件:applicationContext.getBean(BookDao.class);找到就赋值
 *        2）、如果找到多个相同类型的组件，再将属性的名称作为组件的id去容器中查找
 *                       applicationContext.getBean("bookDao")
 *        3）、@Qualifier("bookDao")：使用@Qualifier指定需要装配的组件的id，而不是使用属性名
 *        4）、自动装配默认一定要将属性赋值好，没有就会报错；
 *             可以使用@Autowired(required=false);
 *        5）、@Primary：让Spring进行自动装配的时候，默认使用首选的bean；
 *              也可以继续使用@Qualifier指定需要装配的bean的名字
 *        
 * 2）、Spring还支持使用@Resource(JSR250)和@Inject(JSR330)[java规范的注解]
 *        @Resource:
 *           可以和@Autowired一样实现自动装配功能；默认是按照组件名称进行装配的；
 *           没有能支持@Primary功能没有支持@Autowired（reqiured=false）;
 *        @Inject:
 *           需要导入javax.inject的包，和Autowired的功能一样。没有required=false的功能；
 *  区别： @Autowired:Spring定义的，按类型装配； @Resource、@Inject都是java规范，按name装配
 *     
 * AutowiredAnnotationBeanPostProcessor:解析完成自动装配功能；       
 * 
 * 3）、 @Autowired:构造器，参数，方法，属性；都是从容器中获取参数组件的值
 *        1）、[标注在方法位置]：@Bean+方法参数；参数从容器中获取;默认不写@Autowired效果是一样的；都能自动装配
 *        2）、[标在构造器上]：如果组件只有一个有参构造器，这个有参构造器的@Autowired可以省略，参数位置的组件还是可以自动从容器中获取
 *        3）、放在参数位置：
 * 
 * 4）、自定义组件想要使用Spring容器底层的一些组件（ApplicationContext，BeanFactory，xxx）；
 *        自定义组件实现xxxAware；在创建对象的时候，会调用接口规定的方法注入相关组件；Aware；
 *        把Spring底层一些组件注入到自定义的Bean中；
 *        xxxAware：功能使用xxxProcessor来处理；
 *        ApplicationContextAware ==》 ApplicationContextAwareProcessor；
 
       Aware 的接口
          ApplicationContextAware        -->获取ApplicationContext 
          ApplicationEventPublisherAware -->获取ApplicationEventPublisher  事件派发器
          BeanClassLoaderAware           -->获取 ClassLoader    类加载器
          BeanFactoryAware               -->获取 BeanFactory   bean工厂
          BeanNameAware                  -->获取 beanName 
          EmbeddedValueResolverAware     -->获取 值解析器,解析占位符
          EnvironmentAware               -->获取 Environment  运行环境
          ImportAware                    -->获取 AnnotationMetadata   导入相关bean
          ResourceLoaderAware            -->获取 ResourceLoader   资源加载
          MessageSourceAware             -->获取 MessageSource    国际化
*/        
 
```

1.Autowired和Qualifier注解  (AutowiredAnnotationBeanPostProcessor处理，解析完成自动装配的功能)

~~~java
@Qualifier("persionDao")   //使用@Qualifier指定需要装配的组件的id，而不是使用属性名
@Autowired(required = false)  //有persionDao这个bean就注解没有不报错
public PersionDao persionDao;


@Primary  //@Primary：让Spring进行自动装配的时候，默认使用首选的bean；多个bean优先选这个bean
@Bean
public PersionDao persionDao(){
    return new PersionDao();
}

~~~

2、Aware使用，实现相应的接口

~~~java
@Component
public class Red implements ApplicationContextAware,BeanNameAware,EmbeddedValueResolverAware {
	
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// TODO Auto-generated method stub
		System.out.println("传入的ioc："+applicationContext);
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanName(String name) {
		// TODO Auto-generated method stub
		System.out.println("当前bean的名字："+name);
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		// TODO Auto-generated method stub
		String resolveStringValue = resolver.resolveStringValue("你好 ${os.name} 我是 #{20*18}");
		System.out.println("解析的字符串："+resolveStringValue);
	}
}

~~~



