## Spring boot Aop面向切面编程

AOP：【动态代理】
* 指在程序运行期间动态的将某段代码切入到指定方法指定位置进行运行的编程方式；

  ### 一、添加pom依赖

  ~~~xml
  <!--- AOP -->
  <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-aspects</artifactId>
      <version>4.3.12.RELEASE</version>
  </dependency>
  
  <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-context</artifactId>
      <version>4.3.12.RELEASE</version>
  </dependency>
  
  ~~~

  ### 二、切面通知类型：AbstractAspectJAdvice

  -  前置通知(@Before)：在目标方法运行之前运行
  -   后置通知(@After)：在目标方法运行结束之后运行（无论方法正常结束还是异常结束）

   * 返回通知(@AfterReturning)：在目标方法正常返回之后运行 AspectJAfterReturningAdvice
   * 异常通知(@AfterThrowing)： 在目标方法出现异常以后运行
   * 环绕通知(@Around)：动态代理，手动推进目标方法运行（joinPoint.procced()）

    

    

    **切面类上添加：@Aspect 注解**


  ~~~java
   @Aspect
  public class LogAspects {
  
      //抽取公共的切入点表达式
      //1、本类引用
      //2、其他的切面引用
      @Pointcut("execution(* * com.willow.aop.Calculate.*(..))")
      public void pointCut(){};
      //前置通知
      @Before("execution(* com.willow.aop.Calculate.*(..) )")
      public void logStart(JoinPoint joinPoint){
          Object[] args = joinPoint.getArgs();
          System.out.println("****"+joinPoint.getSignature().getName()+"运行。。。@Before:参数列表是：{"+ Arrays.asList(args)+"}");
  
      }
      //后置通知
      @After("execution(* com.willow.aop.Calculate.*(..) )")
      public void logEnd(JoinPoint joinPoint){
          Object[] args = joinPoint.getArgs();
          System.out.println("****"+joinPoint.getSignature().getName()+"运行。。。@After:参数列表是：{"+ Arrays.asList(args)+"}");
  
      }
      //返回通知
      @AfterReturning(value="execution(* com.willow.aop.Calculate.*(..) )",returning = "result")
      public void logReturn(JoinPoint joinPoint,Object result){
          Object[] args = joinPoint.getArgs();
          System.out.println("方法返回"+joinPoint.getSignature().getName()+"运行。。。@result:返回：{"+ result+"}");
      }
      //异常通知
      @AfterThrowing(value="execution(* com.willow.aop.Calculate.*(..) )",throwing="exception")
      public void logThrowing(JoinPoint joinPoint,Exception exception){
          Object[] args = joinPoint.getArgs();
          System.out.println("方法返回异常"+joinPoint.getSignature().getName()+"运行。。。@AfterThrowing:参数列表是：{"+ Arrays.asList(args)+"}");
      }
  
  }
  ~~~

  ### 三、开启AOP模式

  ~~~java
  @Configuration
  @ComponentScan("com.willow.aop")
  @EnableAspectJAutoProxy   //开启注解的AOP模式
  public class AopConfig {
      @Bean
      public LogAspects LogAspects(){   //注册到IOC容器中
          return  new LogAspects();
      }
  }
  
  ~~~

  

###     四、AOP原理

####    4.1查看@EnableAspectJAutoProxy，注解源码：

~~~java
@Import(AspectJAutoProxyRegistrar.class)：//给容器中导入AspectJAutoProxyRegistrar
~~~

####   4.2 AspectJAutoProxyRegistrar注册bean

​    利用AspectJAutoProxyRegistrar自定义给容器中注册bean；AnnotationAwareAspectJAutoProxyCreator（自动代理功能）

​    断点查看执行流程

```java
@Override
public void registerBeanDefinitions(  //注册bean 到容器中
    AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    //注册bean :AnnotationAwareAspectJAutoProxyCreator
    AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
    //获取EnableAspectJAutoProxy 注解的信息
    AnnotationAttributes enableAspectJAutoProxy =
        AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
    if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
        AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
    }
    if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
        AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
    }
}
```

执行流程：

~~~java

AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
//第一步：
public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
}
//第二步： 注册AnnotationAwareAspectJAutoProxyCreator 类到容器中去
public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object source) {
		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);   //注册或者升级 
}
//第三步：
private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry, Object source) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
  //如果容器中存在这个org.springframework.aop.config.internalAutoProxyCreator //第一次没有   
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				if (currentPriority < requiredPriority) {
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			return null;
		}
    //容器中注册  AnnotationAwareAspectJAutoProxyCreator 这个bean定义
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    //注册名称为：org.springframework.aop.config.internalAutoProxyCreator 的 AnnotationAwareAspectJAutoProxyCreator 定义，并未创建到容器
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
}

~~~

####4.3 注册的bean：AnnotationAwareAspectJAutoProxyCreator  探索

AnnotationAwareAspectJAutoProxyCreator继承关系：

~~~java
  AnnotationAwareAspectJAutoProxyCreator：
 		AnnotationAwareAspectJAutoProxyCreator
   			->AspectJAwareAdvisorAutoProxyCreator
   				->AbstractAdvisorAutoProxyCreator
   					->AbstractAutoProxyCreator
   					 implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware
  						//关注后置处理器（在bean初始化完成前后做事情）、自动装配BeanFactory
 // 重点处理在：
  public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware
		
  SmartInstantiationAwareBeanPostProcessor  //(bean的后置处理器 PostProcessor)	
  BeanFactoryAware    //获取beanFactory 功能
  
// 重点处理方法：  
1. AnnotationAwareAspectJAutoProxyCreator.initBeanFactory()；

2.AbstractAdvisorAutoProxyCreator.setBeanFactory()-> initBeanFactory()；
  
3.AbstractAutoProxyCreator.setBeanFactory();
      
4.AbstractAutoProxyCreator.postProcessBeforeInstantiation()//有后置处理器的逻辑；
  

  
 
  						
~~~

