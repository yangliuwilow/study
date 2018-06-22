# Spring bean的生命周期

bean创建---初始化----销毁的过程
    容器管理bean的生命周期；
  我们可以自定义初始化和销毁方法；容器在bean进行到当前生命周期的时候来调用我们自定义的初始化和销毁方法

  构造（对象创建）
      单实例：在容器启动的时候创建对象
      多实例：在每次获取的时候创建对象

### 1）、指定初始化和销毁方法

​       通过@Bean指定init-method和destroy-method；

~~~java
@Bean(initMethod="init",destroyMethod = "destory")  //指定init(),和destory()方法，
public Persion persion() {
    System.out.println("给容器中添加Person....");
    return new Persion(1, "willow", "28");
}
//initMethod在bean初始化后调用
~~~

###  2）、通过让Bean实现InitializingBean（定义初始化逻辑）  DisposableBean（定义销毁逻辑）;   

~~~java
public class Color  implements InitializingBean,DisposableBean {
    @Override
    public void destroy() throws Exception {
        System.out.println("color ....disposableBean");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("color ....afterPropertiesSet");
    }
}
~~~

###  3）、可以使用JSR250；@PostConstruct和@PreDestroy注解

* @PostConstruct：在bean创建完成并且属性赋值完成；来执行初始化方法

* @PreDestroy：在容器销毁bean之前通知我们进行清理工作

  ~~~java
  @Component
  public class Red {
      
      @PostConstruct
      public void init(){
          System.out.println("red.....init.......");
      }
  
      @PreDestroy()
      public void destory(){
          System.out.println("red.....destory.............");
      }
  }
  
  ~~~
###4）、实现BeanPostProcessor【interface】：bean的后置处理器；

  *     在bean初始化前后进行一些处理工作；
  *     postProcessBeforeInitialization:在初始化之前工作
  *     postProcessAfterInitialization:在初始化之后工作

​        Spring底层对 BeanPostProcessor 的使用；

bean赋值，注入其他组件，

- @Async          （AsyncAnnotationBeanPostProcessor）
- @Autowired （AutowiredAnnotationBeanPostProcessor）
- InitDestroyAnnotationBeanPostProcessor
- ApplicationContextAwareProcessor   //实现ApplicationContextAware接口帮我组件注入IOC容器
- InitDestroyAnnotationBeanPostProcessor   //处理@PostConstruct和@PreDestroy 的
- BeanValidationPostProcessor  数据校验处理

类中所有的注解，都可以看看有没有对应的BeanPostProcessor 组件

​     

  生命周期注解功能，@Async,xxx BeanPostProcessor;

~~~java
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("postProcessBeforeInitialization..."+beanName+"=>"+bean);
        return bean;
    }

    //初始化之后后置处理
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("postProcessAfterInitialization..."+beanName+"=>"+bean);
        return bean;
    }
}
~~~



### BeanPostProcessor执行流程和源码分析：

MyBeanPostProcessor.postProcessAfterInitialization()方法上断点查看执行流程,(自定义的这个方法上)

```java
遍历得到容器中所有的BeanPostProcessor；挨个执行beforeInitialization，
* 一但返回null，跳出for循环，不会执行后面的BeanPostProcessor.postProcessorsBeforeInitialization
* 
* BeanPostProcessor原理
* populateBean(beanName, mbd, instanceWrapper);给bean进行属性赋值
* initializeBean
* {
* applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName); //前置
* invokeInitMethods(beanName, wrappedBean, mbd);//执行自定义初始化
* applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName); //后置
*}
```



```java
//  AbstractAutowireCapableBeanFactory.doCreateBean()
try {
   populateBean(beanName, mbd, instanceWrapper); //属性赋值
   if (exposedObject != null) {
      exposedObject = initializeBean(beanName, exposedObject, mbd);
   }
}
```

AbstractAutowireCapableBeanFactory.initializeBean()
~~~java


protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				@Override
				public Object run() {
					invokeAwareMethods(beanName, bean);
					return null;
				}
			}, getAccessControlContext());
		}
		else {
			invokeAwareMethods(beanName, bean);
		}

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {  //初始化之前执行
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			invokeInitMethods(beanName, wrappedBean, mbd); //初始化
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null),
					beanName, "Invocation of init method failed", ex);
		}

		if (mbd == null || !mbd.isSynthetic()) {//初始化之后执行
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}
		return wrappedBean;
	}
~~~

初始化之前执行

~~~java
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {
		Object result = existingBean;
 //   遍历得到容器中所有的BeanPostProcessor；挨个执行beforeInitialization，
// 一但返回null，跳出for循环，不会执行后面的BeanPostProcessor.postProcessorsBeforeInitialization
		for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {//获取 
			result = beanProcessor.postProcessBeforeInitialization(result, beanName);
			if (result == null) {
				return result;
			}
		}
		return result;
	}
~~~

