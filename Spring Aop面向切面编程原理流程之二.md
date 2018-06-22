```java
流程：
*     1）、传入配置类，创建ioc容器
*     2）、注册配置类，调用refresh（）刷新容器；AbstractApplicationContext.refresh()
*     3）、registerBeanPostProcessors(beanFactory);注册bean的后置处理器来方便拦截bean的创建；
*        1）、先获取ioc容器已经定义了的需要创建对象的所有BeanPostProcessor
*        2）、给容器中加别的BeanPostProcessor
*        3）、优先注册实现了PriorityOrdered接口的BeanPostProcessor；
*        4）、再给容器中注册实现了Ordered接口的BeanPostProcessor；
*        5）、注册没实现优先级接口的BeanPostProcessor；
*        6）、注册BeanPostProcessor，实际上就是创建BeanPostProcessor对象，保存在容器中；
*           创建internalAutoProxyCreator的BeanPostProcessor【AnnotationAwareAspectJAutoProxyCreator】
*           1）、创建Bean的实例
*           2）、populateBean；给bean的各种属性赋值
*           3）、initializeBean：初始化bean；
*                 1）、invokeAwareMethods()：处理Aware接口的方法回调
*                 2）、applyBeanPostProcessorsBeforeInitialization()：应用后置处理器的postProcessBeforeInitialization（）
*                 3）、invokeInitMethods()；执行自定义的初始化方法
*                 4）、applyBeanPostProcessorsAfterInitialization()；执行后置处理器的postProcessAfterInitialization（）；
*           4）、BeanPostProcessor(AnnotationAwareAspectJAutoProxyCreator)创建成功；--》aspectJAdvisorsBuilder
*        7）、把BeanPostProcessor注册到BeanFactory中；
*           beanFactory.addBeanPostProcessor(postProcessor);
* =======以上是创建和注册AnnotationAwareAspectJAutoProxyCreator的过程========
* 
*        AnnotationAwareAspectJAutoProxyCreator => InstantiationAwareBeanPostProcessor
*     4）、finishBeanFactoryInitialization(beanFactory);完成BeanFactory初始化工作；创建剩下的单实例bean
*        1）、遍历获取容器中所有的Bean，依次创建对象getBean(beanName);
*           getBean->doGetBean()->getSingleton()->
*        2）、创建bean
*           【AnnotationAwareAspectJAutoProxyCreator在所有bean创建之前会有一个拦截，InstantiationAwareBeanPostProcessor，会调用postProcessBeforeInstantiation()】
*           1）、先从缓存中获取当前bean，如果能获取到，说明bean是之前被创建过的，直接使用，否则再创建；
*              只要创建好的Bean都会被缓存起来
*           2）、createBean（）;创建bean；
*              AnnotationAwareAspectJAutoProxyCreator 会在任何bean创建之前先尝试返回bean的实例
*              【BeanPostProcessor是在Bean对象创建完成初始化前后调用的】
*              【InstantiationAwareBeanPostProcessor是在创建Bean实例之前先尝试用后置处理器返回对象的】
*              1）、resolveBeforeInstantiation(beanName, mbdToUse);解析BeforeInstantiation
*                 希望后置处理器在此能返回一个代理对象；如果能返回代理对象就使用，如果不能就继续
*                 1）、后置处理器先尝试返回对象；
*                    bean = applyBeanPostProcessorsBeforeInstantiation（）：
*                       拿到所有后置处理器，如果是InstantiationAwareBeanPostProcessor;
*                       就执行postProcessBeforeInstantiation
*                    if (bean != null) {
                     bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                  }
* 
*              2）、doCreateBean(beanName, mbdToUse, args);真正的去创建一个bean实例；和3.6流程一样；
*              3）、
*        
*     
* AnnotationAwareAspectJAutoProxyCreator【InstantiationAwareBeanPostProcessor】  的作用：
* 1）、每一个bean创建之前，调用postProcessBeforeInstantiation()；
*     关心MathCalculator和LogAspect的创建
*     1）、判断当前bean是否在advisedBeans中（保存了所有需要增强bean）
*     2）、判断当前bean是否是基础类型的Advice、Pointcut、Advisor、AopInfrastructureBean，
*        或者是否是切面（@Aspect）
*     3）、是否需要跳过
*        1）、获取候选的增强器（切面里面的通知方法）【List<Advisor> candidateAdvisors】
*           每一个封装的通知方法的增强器是 InstantiationModelAwarePointcutAdvisor；
*           判断每一个增强器是否是 AspectJPointcutAdvisor 类型的；返回true
*        2）、永远返回false
* 
* 2）、创建对象
* postProcessAfterInitialization；
*     return wrapIfNecessary(bean, beanName, cacheKey);//包装如果需要的情况下
*     1）、获取当前bean的所有增强器（通知方法）  Object[]  specificInterceptors
*        1、找到候选的所有的增强器（找哪些通知方法是需要切入当前bean方法的）
*        2、获取到能在bean使用的增强器。
*        3、给增强器排序
*     2）、保存当前bean在advisedBeans中；
*     3）、如果当前bean需要增强，创建当前bean的代理对象；
*        1）、获取所有增强器（通知方法）
*        2）、保存到proxyFactory
*        3）、创建代理对象：Spring自动决定
*           JdkDynamicAopProxy(config);jdk动态代理；
*           ObjenesisCglibAopProxy(config);cglib的动态代理；
*     4）、给容器中返回当前组件使用cglib增强了的代理对象；
*     5）、以后容器中获取到的就是这个组件的代理对象，执行目标方法的时候，代理对象就会执行通知方法的流程；
*     
*  
*  3）、目标方法执行  ；
*     容器中保存了组件的代理对象（cglib增强后的对象），这个对象里面保存了详细信息（比如增强器，目标对象，xxx）；
*     1）、CglibAopProxy.intercept();拦截目标方法的执行
*     2）、根据ProxyFactory对象获取将要执行的目标方法拦截器链；
*        List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
*        1）、List<Object> interceptorList保存所有拦截器 5
*           一个默认的ExposeInvocationInterceptor 和 4个增强器；
*        2）、遍历所有的增强器，将其转为Interceptor；
*           registry.getInterceptors(advisor);
*        3）、将增强器转为List<MethodInterceptor>；
*           如果是MethodInterceptor，直接加入到集合中
*           如果不是，使用AdvisorAdapter将增强器转为MethodInterceptor；
*           转换完成返回MethodInterceptor数组；
* 
*     3）、如果没有拦截器链，直接执行目标方法;
*        拦截器链（每一个通知方法又被包装为方法拦截器，利用MethodInterceptor机制）
*     4）、如果有拦截器链，把需要执行的目标对象，目标方法，
*        拦截器链等信息传入创建一个 CglibMethodInvocation 对象，
*        并调用 Object retVal =  mi.proceed();
*     5）、拦截器链的触发过程;
*        1)、如果没有拦截器执行执行目标方法，或者拦截器的索引和拦截器数组-1大小一样（指定到了最后一个拦截器）执行目标方法；
*        2)、链式获取每一个拦截器，拦截器执行invoke方法，每一个拦截器等待下一个拦截器执行完成返回以后再来执行；
*           拦截器链的机制，保证通知方法与目标方法的执行顺序；
*     
*  总结：
*     1）、  @EnableAspectJAutoProxy 开启AOP功能
*     2）、 @EnableAspectJAutoProxy 会给容器中注册一个组件 AnnotationAwareAspectJAutoProxyCreator
*     3）、AnnotationAwareAspectJAutoProxyCreator是一个后置处理器；
*     4）、容器的创建流程：
*        1）、registerBeanPostProcessors（）注册后置处理器；创建AnnotationAwareAspectJAutoProxyCreator对象
*        2）、finishBeanFactoryInitialization（）初始化剩下的单实例bean
*           1）、创建业务逻辑组件和切面组件
*           2）、AnnotationAwareAspectJAutoProxyCreator拦截组件的创建过程
*           3）、组件创建完之后，判断组件是否需要增强
*              是：切面的通知方法，包装成增强器（Advisor）;给业务逻辑组件创建一个代理对象（cglib）；
*     5）、执行目标方法：
*        1）、代理对象执行目标方法
*        2）、CglibAopProxy.intercept()；
*           1）、得到目标方法的拦截器链（增强器包装成拦截器MethodInterceptor）
*           2）、利用拦截器的链式机制，依次进入每一个拦截器进行执行；
*           3）、效果：
*              正常执行：前置通知-》目标方法-》后置通知-》返回通知
*              出现异常：前置通知-》目标方法-》后置通知-》异常通知
*     
* 
* 
*/
```

###  

断点位置：

 AbstractAutoProxyCreator.setBeanFactory()

 AbstractAutoProxyCreator.postProcessBeforeInstantiation()                  //有后置处理器的逻辑；

 AbstractAdvisorAutoProxyCreator.setBeanFactory()->initBeanFactory()  //第一个断点这个位置

 AnnotationAwareAspectJAutoProxyCreator.initBeanFactory()

![1529154800669](images\springAop断点过程.png)



##  1）、传入配置类，创建ioc容器

~~~java
public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);//注册配置类
		refresh();
}
~~~



## 2）、注册配置类，调用refresh（）刷新容器；

```java
AbstractApplicationContext.refresh() //刷新
```

##3）、注册bean的后置处理器来方便拦截bean的创建

  refresh()中的registerBeanPostProcessors(beanFactory);  

```java
// Register bean processors that intercept bean creation.
registerBeanPostProcessors(beanFactory);
```

### 3.1  注册BeanPostProcessors后置处理器

```java
PostProcessorRegistrationDelegate.registerBeanPostProcessors()//方法中
    
public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		//先获取ioc容器已经定义了的需要创建对象的所有BeanPostProcessor（未创建只是定义了的）
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
    //org.springframework.aop.config.internalAutoProxyCreator  就是之前注册的AnnotationAwareAspectJAutoProxyCreator

	 
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
    // 分别分离 BeanPostProcessors 实现了 PriorityOrdered,Ordered, and the rest. 的接口
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}
        
		// 优先注册实现了PriorityOrdered接口的BeanPostProcessor；
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// 再注册实现了Ordered接口的BeanPostProcessor；
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : orderedPostProcessorNames) {
            //获取bean ,没有bean就创建这个bean
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
         //把BeanPostProcessor注册到BeanFactory中beanFactory.addBeanPostProcessor(postProcessor);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// 再注册没实现优先级接口的BeanPostProcessor；
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}


```

### 3.4 再给容器中注册实现了Ordered接口的BeanPostProcessor；

### 3.5 注册没实现优先级接口的BeanPostProcessor；

### 3.6 注册BeanPostProcessor

  实际上就是创建BeanPostProcessor对象，保存在容器中；

  创建internalAutoProxyCreator的BeanPostProcessor【AnnotationAwareAspectJAutoProxyCreator】

* 1）、创建Bean的实例

  ```java
  //获取 getBean()->创建 doCreateBean()->do
  BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
  @Override
  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
      return doGetBean(name, requiredType, null, false);
  }	
  ```

* 2）、populateBean；给bean的各种属性赋值  AbstractAutowireCapableBeanFactory.doCreateBean()->populateBean()

* 3）、initializeBean：初始化bean；  AbstractAutowireCapableBeanFactory.doCreateBean()-initializeBean()

  

  ```java
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
        // 1.处理Aware接口的方法回调  
        invokeAwareMethods(beanName, bean);
     }
  
     Object wrappedBean = bean;
     if (mbd == null || !mbd.isSynthetic()) {
        //2 .应用后置处理器的postProcessBeforeInitialization()方法,就是bean的before()
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
     }
  
     try {
         //3.执行自定义的初始化方法 ,执行setBeanFactory()方法
        invokeInitMethods(beanName, wrappedBean, mbd);
     }
     catch (Throwable ex) {
        throw new BeanCreationException(
              (mbd != null ? mbd.getResourceDescription() : null),
              beanName, "Invocation of init method failed", ex);
     }
  
     if (mbd == null || !mbd.isSynthetic()) {
         //4 .应用后置处理器的postProcessAfterInitialization()方法,就是bean的After()
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
     }
     return wrappedBean;
  }
  ```

  

  * 1）、invokeAwareMethods处理Aware接口的方法回调  

    ```java
    //AbstractAutowireCapableBeanFactory
    private void invokeAwareMethods(final String beanName, final Object bean) {
       if (bean instanceof Aware) {
          if (bean instanceof BeanNameAware) {
             ((BeanNameAware) bean).setBeanName(beanName);
          }
          if (bean instanceof BeanClassLoaderAware) {
             ((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
          }
           //执行 AnnotationAwareAspectJAutoProxyCreator 的setBeanFactory()方法
          if (bean instanceof BeanFactoryAware) {
             ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
          }
       }
    }
    ```

  * 2）、applyBeanPostProcessorsBeforeInitialization()：执行应用后置处理器的postProcessBeforeInitialization（）方法

    ```java
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
          throws BeansException {
    
       Object result = existingBean;
       for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
          result = beanProcessor.postProcessBeforeInitialization(result, beanName);
          if (result == null) {
             return result;
          }
       }
       return result;
    }
    ```

  * 3）、invokeInitMethods()；执行自定义的初始化方法

    

  * 4）、applyBeanPostProcessorsAfterInitialization()；执行后置处理器的postProcessAfterInitialization（）；

* 4）、BeanPostProcessor(AnnotationAwareAspectJAutoProxyCreator)创建成功；-->aspectJAdvisorsBuilder

### 3.7   把BeanPostProcessor注册到BeanFactory中  ,注册完成 

```java
 //beanFactory.addBeanPostProcessor(postProcessor);

registerBeanPostProcessors(beanFactory, orderedPostProcessors);
```

​    AnnotationAwareAspectJAutoProxyCreator => InstantiationAwareBeanPostProcessor



##  4）、finishBeanFactoryInitialization(beanFactory);

断点在：AbstractAutoProxyCreator->postProcessBeforeInstantiation()方法上

![aop-postProcess](images\aop-postProcess.png)

AbstractApplicationContext.refresh()-->finishBeanFactoryInitialization(beanFactory); 方法中

完成BeanFactory初始化工作；创建剩下的单实例bean    

createBean：

~~~java

//Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
//希望后置处理器在此能返回一个代理对象；如果能返回代理对象就使用，如果不能就继续doCreateBean(beanName, mbdToUse, args);真正的去创建一个bean实例； 
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				Class<?> targetType = determineTargetType(beanName, mbd);
				if (targetType != null) {
                     // 后置处理器先尝试返回对象；
					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
					if (bean != null) {
						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
				}
			}
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
}

protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
// 拿到所有后置处理器，如果是InstantiationAwareBeanPostProcessor;就执行postProcessBeforeInstantiation()这个方法
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
                // AbstractAutoProxyCreator 实现了SmartInstantiationAwareBeanPostProcessor 接口，SmartInstantiationAwareBeanPostProcessor继承了InstantiationAwareBeanPostProcessor
				InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                //此时就是调用AbstractAutoProxyCreator.postProcessBeforeInstantiation()方法
				Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
~~~



后置处理器的区别：

```java
 AnnotationAwareAspectJAutoProxyCreator在所有bean创建之前会有一个拦截，会调用postProcessBeforeInstantiation() 
 AnnotationAwareAspectJAutoProxyCreator 会在任何bean创建之前先尝试返回bean的实例 
【BeanPostProcessor是在Bean对象创建完成初始化前后调用的】
【InstantiationAwareBeanPostProcessor是在创建任何Bean实例之前先尝试用后置处理器返回对象的】
```
## 5. AbstractAutoProxyCreator 后置处理器

#### 5.1postProcessBeforeInstantiation()后置处理器功能

断点到：AbstractAutoProxyCreator.postProcessBeforeInstantiation()中，只关心MathCalculator和LogAspect的创建，断点到这个2个对应的beanClass

1）、每一个bean创建之前，调用postProcessBeforeInstantiation()；

 * 		关心MathCalculator和LogAspect的创建
  * 		1）、判断当前bean是否在advisedBeans中（保存了所有需要增强bean）
       2）、判断当前bean是否是基础类型的Advice、Pointcut、Advisor、AopInfrastructureBean，
        			或者是否是切面（@Aspect）
        		3）、是否需要跳过
        			1）、获取候选的增强器（切面里面的通知方法）【List<Advisor> candidateAdvisors】
        				每一个封装的通知方法的增强器是 InstantiationModelAwarePointcutAdvisor；
        				判断每一个增强器是否是 AspectJPointcutAdvisor 类型的；返回true
        			2）、永远返回false

~~~java

public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (beanName == null || !this.targetSourcedBeans.contains(beanName)) {
            //1）、判断当前bean是否在advisedBeans中（保存了所有需要增强bean）
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
            //2）、isInfrastructureClass(beanClass) 判断当前bean是否是基础类型的Advice、Pointcut、Advisor、AopInfrastructureBean，或者是否是切面（@Aspect）
            //、shouldSkip()是否需要跳过
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		 
		if (beanName != null) {
            //获取自定义的TargetSource
			TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
			if (targetSource != null) {
				this.targetSourcedBeans.add(beanName);
				Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
				Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
				this.proxyTypes.put(cacheKey, proxy.getClass());
				return proxy;
			}
		}

		return null;
	}
~~~

3.1shouldSkip 说明

~~~java
protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// 找到候选的增强器，就是切面里面的通知方法
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		for (Advisor advisor : candidateAdvisors) {
 //  每一个封装的通知方法的增强器是 InstantiationModelAwarePointcutAdvisor类型的；判断每一个增强器是否是 AspectJPointcutAdvisor 类型的；返回true,当前放回false
			if (advisor instanceof AspectJPointcutAdvisor) {
				if (((AbstractAspectJAdvice) advisor.getAdvice()).getAspectName().equals(beanName)) {
					return true;
				}
			}
		}
    //2）、永远返回false
		return super.shouldSkip(beanClass, beanName);
	}
~~~

#### 5.2 AbstractAutoProxyCreator.postProcessAfterInitialization()

postProcessBeforeInstantiation 处理完成后进入postProcessAfterInitialization()方法中

 * 2）、创建对象
 * postProcessAfterInitialization；
  * 		return wrapIfNecessary(bean, beanName, cacheKey);//包装如果需要的情况下
       1）、获取当前bean的所有增强器（通知方法）  Object[]  specificInterceptors
        			1、找到候选的所有的增强器（找哪些通知方法是需要切入当前bean方法的）
                       //AbstractAdvisorAutoProxyCreator.findEligibleAdvisors()中
        			2、获取到能在bean使用的增强器。
        			3、给增强器排序
        		2）、保存当前bean在advisedBeans中；
        		3）、如果当前bean需要增强，创建当前bean的代理对象；
        			1）、获取所有增强器（通知方法）
        			2）、保存到proxyFactory
        			3）、创建代理对象：Spring自动决定
        				JdkDynamicAopProxy(config);jdk动态代理；
        				ObjenesisCglibAopProxy(config);cglib的动态代理；
        		4）、给容器中返回当前组件使用cglib增强了的代理对象；
        		5）、以后容器中获取到的就是这个组件的代理对象，执行目标方法的时候，代理对象就会执行通知方法的流程；

~~~java
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.contains(cacheKey)) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
}

protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
   		  //是否切面
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// 创建一个代理对象
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
             //2）、保存当前bean在advisedBeans中；
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
            //3）、如果当前bean需要增强，创建当前bean的代理对象；
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}
~~~

## 6.拦截器目标方法执行

 * 容器中保存了组件的代理对象（cglib增强后的对象），这个对象里面保存了详细信息（比如增强器，目标对象，xxx）；
  * 1）、CglibAopProxy.intercept();拦截目标方法的执行
       2）、根据ProxyFactory对象获取将要执行的目标方法拦截器链；
        			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
        			1）、List<Object> interceptorList保存所有拦截器 5个长度
        				一个默认的ExposeInvocationInterceptor 和 4个增强器；
        			2）、遍历所有的增强器，将其转为Interceptor；
        				registry.getInterceptors(advisor);
        			3）、将增强器转为List<MethodInterceptor>；

       ​                             在  这个DefaultAdvisorAdapterRegistry. getInterceptors()方法中处理

         				如果是MethodInterceptor，直接加入到集合中
        				如果不是，使用AdvisorAdapter将增强器转为MethodInterceptor；
        				转换完成返回MethodInterceptor数组；

 * 3）、如果没有拦截器链，直接执行目标方法;
  拦截器链（每一个通知方法又被包装为方法拦截器，利用MethodInterceptor机制）

 * 4）、如果有拦截器链，把需要执行的目标对象，目标方法，
   			拦截器链等信息传入创建一个 CglibMethodInvocation 对象，
   			并调用 Object retVal =  mi.proceed();

 * 5）、拦截器链的触发过程;
   			1)、如果没有拦截器执行执行目标方法，或者拦截器的索引和拦截器数组-1大小一样（指定到了最后一个拦截器）执行目标方法；

  ```java
  if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
     return invokeJoinpoint();
  }
  ```

   			2)、链式获取每一个拦截器，拦截器执行invoke方法，每一个拦截器等待下一个拦截器执行完成返回以后再来执行；
   				拦截器链的机制，保证通知方法与目标方法的执行顺序；

~~~java
 Calculate calculate=  applicationContext.getBean(Calculate.class);
 calculate.add(1,2);//断点到这个位置，下一步

//CglibAopProxy类中
public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Object oldProxy = null;
			boolean setProxyContext = false;
			Class<?> targetClass = null;
			Object target = null;
			try {
				if (this.advised.exposeProxy) {
				 
					oldProxy = AopContext.setCurrentProxy(proxy);
					setProxyContext = true;
				}
				 
				target = getTarget();
				if (target != null) {
					targetClass = target.getClass();
				}
                //根据ProxyFactory对象获取将要执行的目标方法拦截器链；
				List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
				Object retVal;
				//如果拦截器链是空的，就直接执行目标方法
				if (chain.isEmpty() && Modifier.isPublic(method.getModifiers())) {
					 
					Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
					retVal = methodProxy.invoke(target, argsToUse);
				}
				else {
					// 如果有拦截器链，把需要执行的目标对象，目标方法，拦截器链等信息传入创建一个 CglibMethodInvocation 对象，并调用 Object retVal =  mi.proceed();

					retVal = new CglibMethodInvocation(proxy, target, method, args, targetClass, chain, methodProxy).proceed();
				}
				retVal = processReturnType(proxy, target, method, retVal);
				return retVal;
			}
			finally {
				if (target != null) {
					releaseTarget(target);
				}
				if (setProxyContext) {
					// Restore old proxy.
					AopContext.setCurrentProxy(oldProxy);
				}
			}
		}
~~~

 总结：

 * 1）、  @EnableAspectJAutoProxy 开启AOP功能
  * 2）、 @EnableAspectJAutoProxy 会给容器中注册一个组件 AnnotationAwareAspectJAutoProxyCreator
       3）、AnnotationAwareAspectJAutoProxyCreator是一个后置处理器；
        		4）、容器的创建流程：
        			1）、registerBeanPostProcessors（）注册后置处理器；创建AnnotationAwareAspectJAutoProxyCreator对象
        			2）、finishBeanFactoryInitialization（）初始化剩下的单实例bean
        				1）、创建业务逻辑组件和切面组件
        				2）、AnnotationAwareAspectJAutoProxyCreator拦截组件的创建过程
        				3）、组件创建完之后，postProcessAfterInitialization判断组件是否需要增强

        					是：切面的通知方法，包装成增强器（Advisor）;给业务逻辑组件创建一个代理对象（cglib）；
        		5）、执行目标方法：
        			1）、代理对象执行目标方法
        			2）、CglibAopProxy.intercept()；
        				1）、得到目标方法的拦截器链（增强器包装成拦截器MethodInterceptor）
        				2）、利用拦截器的链式机制，依次进入每一个拦截器进行执行；
        				3）、效果：
        					正常执行：前置通知-》目标方法-》后置通知-》返回通知
        					出现异常：前置通知-》目标方法-》后置通知-》异常通知