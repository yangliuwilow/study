

Spring 注解  事物@Transactional开发

## 一、声明式事务：

   环境搭建：
   1、导入相关依赖
          数据源、数据库驱动、Spring-jdbc模块

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>4.3.13.RELEASE</version>
</dependency>

<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.44</version>
</dependency>
```

   2、配置数据源、JdbcTemplate（Spring提供的简化数据库操作的工具）操作数据

   3、给方法上标注 @Transactional 表示当前方法是一个事务方法；

```java
@Transactional
public void add() {
    deptDao.insert();
    int i=10/0;
}
```

   4、 @EnableTransactionManagement 开启基于注解的事务管理功能；
          @EnableXXX
   5、配置事务管理器来控制事务;
          @Bean
          public PlatformTransactionManager transactionManager()

代码：

~~~java
package com.willow.tx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@ComponentScan("com.willow.tx")
public class TxConfig {


    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource ds = new DriverManagerDataSource ();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUrl("jdbc:mysql://192.168.7.108/willow");
        ds.setUsername("root");
        ds.setPassword("123456");
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(){
        return  new JdbcTemplate(dataSource());
    }

    @Bean
    public PlatformTransactionManager transactionManager(){
       return new DataSourceTransactionManager(dataSource());
    }

}

~~~

## 二、事物原理部分

###  1）、@EnableTransactionManagement注解

```java
@Import(TransactionManagementConfigurationSelector.class)
```

  利用TransactionManagementConfigurationSelector给容器中会导入下面两个组件

* AutoProxyRegistrar

* ProxyTransactionManagementConfiguration

  ~~~java
  public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {
  	@Override
  	protected String[] selectImports(AdviceMode adviceMode) {
  //AdviceMode在 EnableTransactionManagement注解中 默认为AdviceMode mode() default AdviceMode.PROXY;
  		switch (adviceMode) {
  			case PROXY:
  				return new String[] {AutoProxyRegistrar.class.getName(), ProxyTransactionManagementConfiguration.class.getName()};
  			case ASPECTJ:
  				return new String[] {TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME};
  			default:
  				return null;
  		}
  	}
  
  }
  ~~~

  

###  2）、AutoProxyRegistrar：

给容器中注册一个 InfrastructureAdvisorAutoProxyCreator 组件

```java
1.  AutoProxyRegistrar.registerBeanDefinitions() -->
    
2. 	AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);   -->
    
3.    public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

4.public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object source) {
		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);  //注册了一个InfrastructureAdvisorAutoProxyCreator组件
}
	
```

*        InfrastructureAdvisorAutoProxyCreator：？原理和AOP部分的AnnotationAwareAspectJAutoProxyCreator一样，利用后置处理器机制在对象创建以后，包装对象，返回一个代理对象（增强器），代理对象执行方法利用拦截器链进行调用；

### 3）、ProxyTransactionManagementConfiguration 做了什么？

####    3.1 、给容器中注册事务增强器；

#####        3.1.1）、事务增强器要用事务注解的信息，AnnotationTransactionAttributeSource解析事务注解

~~~java
@Configuration
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

    //事物增强器
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		advisor.setTransactionAttributeSource(transactionAttributeSource());
		advisor.setAdvice(transactionInterceptor());
		advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		return advisor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}
   // 2）、事务拦截器：
   // TransactionInterceptor；保存了事务属性信息，事务管理器；他是一个 MethodInterceptor；
    @Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor() {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(transactionAttributeSource());
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}
}
~~~



#####  3.1.2）、事务拦截器：

MethodInterceptor在目标方法执行的时候；

执行拦截器链；

事务拦截器：

​     1）、先获取事务相关的属性

​     2）、再获取PlatformTransactionManager，如果事先没有添加指定任何transactionmanger最终会从容器中按照类型获取一个PlatformTransactionManager；

​    3）、执行目标方法

如果异常，获取到事务管理器，利用事务管理回滚操作；

如果正常，利用事务管理器，提交事务

 

#####   3.1.2.1 MethodInterceptor在目标方法执行的时候；

TransactionInterceptor.invoke() 方法

```java
@Override
public Object invoke(final MethodInvocation invocation) throws Throwable {
   
Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

   
return invokeWithinTransaction(invocation.getMethod(), targetClass, new InvocationCallback() {
      @Override
      public Object proceedWithInvocation() throws Throwable {
         return invocation.proceed();
      }
   });
}
```



#####  3.1.2.2 TransactionAspectSupport.invokeWithinTransaction()方法



```java
protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final InvocationCallback invocation)
      throws Throwable {

   //  1）、先获取事务相关的属性
   final TransactionAttribute txAttr = getTransactionAttributeSource().getTransactionAttribute(method, targetClass);
   // 2）、再获取PlatformTransactionManager，如果事先没有添加指定任何transactionmanger
   final PlatformTransactionManager tm = determineTransactionManager(txAttr);
    //获取要执行的事物方法
   final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

   if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
      //  创建一个事物
      TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
      Object retVal = null;
      try {
         //事物方法的执行
         retVal = invocation.proceedWithInvocation();
      }
      catch (Throwable ex) {
         // 有异常回滚事物
         completeTransactionAfterThrowing(txInfo, ex);
         throw ex;
      }
      finally {
         cleanupTransactionInfo(txInfo);
      }
       //得到事物管理器，提交事物
      commitTransactionAfterReturning(txInfo);
      return retVal;
   }

   else {
      final ThrowableHolder throwableHolder = new ThrowableHolder();

      // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
      try {
         Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr,
               new TransactionCallback<Object>() {
                  @Override
                  public Object doInTransaction(TransactionStatus status) {
                     TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
                     try {
                        return invocation.proceedWithInvocation();
                     }
                     catch (Throwable ex) {
                        if (txAttr.rollbackOn(ex)) {
                           // A RuntimeException: will lead to a rollback.
                           if (ex instanceof RuntimeException) {
                              throw (RuntimeException) ex;
                           }
                           else {
                              throw new ThrowableHolderException(ex);
                           }
                        }
                        else {
                           // A normal return value: will lead to a commit.
                           throwableHolder.throwable = ex;
                           return null;
                        }
                     }
                     finally {
                        cleanupTransactionInfo(txInfo);
                     }
                  }
               });

         // Check result state: It might indicate a Throwable to rethrow.
         if (throwableHolder.throwable != null) {
            throw throwableHolder.throwable;
         }
         return result;
      }
      catch (ThrowableHolderException ex) {
         throw ex.getCause();
      }
      catch (TransactionSystemException ex2) {
         if (throwableHolder.throwable != null) {
            logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
            ex2.initApplicationException(throwableHolder.throwable);
         }
         throw ex2;
      }
      catch (Throwable ex2) {
         if (throwableHolder.throwable != null) {
            logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
         }
         throw ex2;
      }
   }
}
```

#####  3.1.2.2.1）、获取PlatformTransactionManager

如果事先没有添加指定任何transactionmanger最终会从容器中按照类型获取一个PlatformTransactionManager

```java
protected PlatformTransactionManager determineTransactionManager(TransactionAttribute txAttr) {
   // Do not attempt to lookup tx manager if no tx attributes are set
   if (txAttr == null || this.beanFactory == null) {
      return getTransactionManager();
   }
   //判断是否@Transactional(transactionManager = "")指定了transactionManager
   String qualifier = txAttr.getQualifier();
   if (StringUtils.hasText(qualifier)) {
      return determineQualifiedTransactionManager(qualifier);
   }
   else if (StringUtils.hasText(this.transactionManagerBeanName)) {
      return determineQualifiedTransactionManager(this.transactionManagerBeanName);
   }
   else {
      PlatformTransactionManager defaultTransactionManager = getTransactionManager();
      if (defaultTransactionManager == null) {
         defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
         if (defaultTransactionManager == null) {
            defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
             //在IOC容器中获取PlatformTransactionManager
            this.transactionManagerCache.putIfAbsent(
                  DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
         }
      }
      return defaultTransactionManager;
   }
}
```

























