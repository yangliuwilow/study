Spring 注解加载外部属性

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



