#          Spring 启动流程源码解析之一

# 一、Spring容器的refresh()  

spring  version:4.3.12  ，尚硅谷Spring注解驱动开发—源码部分

```java
//refresh():543, AbstractApplicationContext (org.springframework.context.support) 
public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			//1 刷新前的预处理
			prepareRefresh();
			//2 获取BeanFactory；刚创建的默认DefaultListableBeanFactory
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
			//3 BeanFactory的预准备工作（BeanFactory进行一些设置）
			prepareBeanFactory(beanFactory);

			try {
				// 4 BeanFactory准备工作完成后进行的后置处理工作；
                 // 4.1）、抽象的方法，当前未做处理。子类通过重写这个方法来在BeanFactory创建并预准备完成以后做进一步的设置
				postProcessBeanFactory(beanFactory);  
		/**************************以上是BeanFactory的创建及预准备工作  ****************/
                
				// 5 执行BeanFactoryPostProcessor的方法；
//BeanFactoryPostProcessor：BeanFactory的后置处理器。在BeanFactory标准初始化之后执行的；
//他的重要两个接口：BeanFactoryPostProcessor、BeanDefinitionRegistryPostProcessor
				invokeBeanFactoryPostProcessors(beanFactory);

				//6 注册BeanPostProcessor（Bean的后置处理器）
				registerBeanPostProcessors(beanFactory);

			// 7 initMessageSource();初始化MessageSource组件（做国际化功能；消息绑定，消息解析）；
				initMessageSource();

				// 8 初始化事件派发器
				initApplicationEventMulticaster();

				// 9 子类重写这个方法，在容器刷新的时候可以自定义逻辑；
				onRefresh();

				// 10 给容器中将所有项目里面的ApplicationListener注册进来
 
				registerListeners();
       

				// 11.初始化所有剩下的单实例bean；
				finishBeanFactoryInitialization(beanFactory);

				// 12.完成BeanFactory的初始化创建工作；IOC容器就创建完成；
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}
```



## 1 、prepareRefresh()刷新前的预处理;

~~~java
//刷新前的预处理;
protected void prepareRefresh() {
    this.startupDate = System.currentTimeMillis();
    this.closed.set(false);
    this.active.set(true);

    if (logger.isInfoEnabled()) {
        logger.info("Refreshing " + this);
    }
    // 初始化一些属性设置;子类自定义个性化的属性设置方法；
    initPropertySources();   
    // 校验配置文件的属性，合法性
    getEnvironment().validateRequiredProperties();
    //保存容器中的一些事件
    this.earlyApplicationEvents = new LinkedHashSet<ApplicationEvent>();
}
~~~

## 2、 obtainFreshBeanFactory();创建了一个BeanFactory

 ~~~java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    refreshBeanFactory(); //创建了一个this.beanFactory = new DefaultListableBeanFactory();设置了序列化的ID
    //返回刚才创建的DefaultListableBeanFactory
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (logger.isDebugEnabled()) {
        logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
    }
    return beanFactory;
}
 ~~~

## 3、 BeanFactory的预准备工作

~~~java
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // 1）、设置BeanFactory的类加载器
    beanFactory.setBeanClassLoader(getClassLoader());
    // 1）、设置支持表达式解析器
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    // 2）、添加部分BeanPostProcessor【ApplicationContextAwareProcessor】
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    // 3）、设置忽略的自动装配的接口EnvironmentAware、EmbeddedValueResolverAware、xxx；
   // 这些接口的实现类不能通过类型来自动注入
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
    beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
    beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
    beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

    // 4）、注册可以解析的自动装配；我们能直接在任何组件中自动注入：
    //BeanFactory、ResourceLoader、ApplicationEventPublisher、ApplicationContext
    /* 其他组件中可以通过下面方式直接注册使用
    @autowired 
    BeanFactory beanFactory */
    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ResourceLoader.class, this);
    beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);

    // 5）、添加BeanPostProcessor【ApplicationListenerDetector】后置处理器，在bean初始化前后的一些工作
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

    // 6）、添加编译时的AspectJ；
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        // Set a temporary ClassLoader for type matching.
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // 7）、给BeanFactory中注册一些能用的组件；
    if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
    // 环境信息ConfigurableEnvironment
        beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
    }
    //系统属性，systemProperties【Map<String, Object>】
    if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
    }
    //系统环境变量systemEnvironment【Map<String, Object>】
    if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
    }
}
~~~

## 5 执行BeanFactoryPostProcessor的后置处理器方法（BeanFactory）；

```java
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    //执行BeanFactoryPostProcessor的方法
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

    // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
    // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
    if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }
}
```

### 5.1    执行invokeBeanFactoryPostProcessors

~~~java
public static void invokeBeanFactoryPostProcessors(
    ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

    // Invoke BeanDefinitionRegistryPostProcessors first, if any.
    Set<String> processedBeans = new HashSet<String>();
   // 判断beanFatory类型
    if (beanFactory instanceof BeanDefinitionRegistry) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
        List<BeanDefinitionRegistryPostProcessor> registryProcessors = new LinkedList<BeanDefinitionRegistryPostProcessor>();
        //得到所有的BeanFactoryPostProcessor  遍历
        for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
            if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                BeanDefinitionRegistryPostProcessor registryProcessor =
                    (BeanDefinitionRegistryPostProcessor) postProcessor;
                registryProcessor.postProcessBeanDefinitionRegistry(registry);
                registryProcessors.add(registryProcessor);
            }
            else {
                regularPostProcessors.add(postProcessor);
            }
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        // Separate between BeanDefinitionRegistryPostProcessors that implement
        // PriorityOrdered, Ordered, and the rest.
        List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();

 
        //从容器中获取所有的BeanDefinitionRegistryPostProcessor，
        String[] postProcessorNames =
            beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
        //  第一步，先执行实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessor
        for (String ppName : postProcessorNames) {
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                processedBeans.add(ppName);
            }
        }
        //排序
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        registryProcessors.addAll(currentRegistryProcessors);
        //执行BeanDefinitionRegistryPostProcessor的 postProcessor.postProcessBeanDefinitionRegistry(registry)
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        currentRegistryProcessors.clear();

        // 在执行实现了Ordered顺序接口的BeanDefinitionRegistryPostProcessor；
        postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
        for (String ppName : postProcessorNames) {
            if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                processedBeans.add(ppName);
            }
        }
        //排序
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        registryProcessors.addAll(currentRegistryProcessors);
        //执行postProcessor.postProcessBeanDefinitionRegistry(registry)
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        currentRegistryProcessors.clear();

        // 最后执行没有实现任何优先级或者是顺序接口的BeanDefinitionRegistryPostProcessors；
        boolean reiterate = true;
        while (reiterate) {
            reiterate = false;
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (!processedBeans.contains(ppName)) {
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                    reiterate = true;
                }
            }
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            registryProcessors.addAll(currentRegistryProcessors);
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            currentRegistryProcessors.clear();
        }

        // 现在执行  BeanFactoryPostProcessor的postProcessBeanFactory()方法
        invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
    }

    else {
        // Invoke factory processors registered with the context instance.
        invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
    }

    //再执行BeanFactoryPostProcessor的方法
    //1）、获取所有的BeanFactoryPostProcessor  和执行BeanDefinitionRegistryPostProcessors的接口逻辑一样
    String[] postProcessorNames =
        beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

   
    List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
    List<String> orderedPostProcessorNames = new ArrayList<String>();
    List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
    //把BeanFactoryPostProcessor  分类
    for (String ppName : postProcessorNames) {
        if (processedBeans.contains(ppName)) {
            // skip - already processed in first phase above
        }
        else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
        }
        else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
            orderedPostProcessorNames.add(ppName);
        }
        else {
            nonOrderedPostProcessorNames.add(ppName);
        }
    }

    // 2）、看先执行实现了PriorityOrdered优先级接口的BeanFactoryPostProcessor、
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

    // 3）、在执行实现了Ordered顺序接口的BeanFactoryPostProcessor；
    List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
    for (String postProcessorName : orderedPostProcessorNames) {
        orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

    // 4）、最后执行没有实现任何优先级或者是顺序接口的BeanFactoryPostProcessor；
    List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
    for (String postProcessorName : nonOrderedPostProcessorNames) {
        nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
    beanFactory.clearMetadataCache();
}
~~~

## 6、注册BeanPostProcessor（Bean的后置处理器）

bean的创建拦截器

不同接口类型的BeanPostProcessor；在Bean创建前后的执行时机是不一样的

- BeanPostProcessor、
- DestructionAwareBeanPostProcessor、
- InstantiationAwareBeanPostProcessor、   在createBean()方法中处理的，bean创建之前调用的
- SmartInstantiationAwareBeanPostProcessor、 
- MergedBeanDefinitionPostProcessor【internalPostProcessors】  （bean创建完成后调用，11章节中）

```
registerBeanPostProcessors(beanFactory);
```

~~~java

public static void registerBeanPostProcessors(
    ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
//1）、获取所有的 BeanPostProcessor;后置处理器都默认可以通过PriorityOrdered、Ordered接口来执行优先级
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

     //检查器，检查所有的BeanPostProcessor
    int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
    beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

    // Separate between BeanPostProcessors that implement PriorityOrdered,
    // Ordered, and the rest.
    List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
    List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
    List<String> orderedPostProcessorNames = new ArrayList<String>();
    List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
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

  /*  2）、先注册PriorityOrdered优先级接口的BeanPostProcessor；
			把每一个BeanPostProcessor；添加到BeanFactory中
			beanFactory.addBeanPostProcessor(postProcessor);*/
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

    // 3）、再注册Ordered接口的
    List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
    for (String ppName : orderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        orderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, orderedPostProcessors);

    // 4）、最后注册没有实现任何优先级接口的
    List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
    for (String ppName : nonOrderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        nonOrderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

    // 5）、最终注册MergedBeanDefinitionPostProcessor；
    sortPostProcessors(internalPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, internalPostProcessors);

    // 6）、注册一个ApplicationListenerDetector；判断创建完成的bean是否监听器
   /* 在Bean创建完成后ApplicationListenerDetector.postProcessAfterInitialization()中检查是否是ApplicationListener 类型，如果是applicationContext.addApplicationListener((ApplicationListener<?>) bean);如果是添加到容器中 */
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
}
~~~



## 7、initMessageSource();国际化

初始化MessageSource组件（做国际化功能；消息绑定，消息解析）；

~~~java

protected void initMessageSource() {
  // 1）、获取BeanFactory
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    /*  
    2）、看容器中是否有id为messageSource的，类型是MessageSource的组件
			如果有赋值给messageSource，如果没有自己创建一个DelegatingMessageSource；
			MessageSource作用：取出国际化配置文件中的某个key的值；能按照区域信息获取；
    */
    if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
        this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
        // Make MessageSource aware of parent MessageSource.
        if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
            HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
            if (hms.getParentMessageSource() == null) {
                // Only set parent context as parent MessageSource if no parent MessageSource
                // registered already.
                hms.setParentMessageSource(getInternalParentMessageSource());
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Using MessageSource [" + this.messageSource + "]");
        }
    }
    else {
        // 如果没有自己创建一个DelegatingMessageSource；
        DelegatingMessageSource dms = new DelegatingMessageSource();
        dms.setParentMessageSource(getInternalParentMessageSource());
        this.messageSource = dms;
        //把创建好的messageSource注册到容器中，以后获取国际化配置文件的值的时候，可以自动注入MessageSource； 
       //注入后通过这个方法使用MessageSource.getMessage(String code, Object[] args, String defaultMessage, Locale locale);
        beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
        if (logger.isDebugEnabled()) {
            logger.debug("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
                         "': using default [" + this.messageSource + "]");
        }
    }
}
~~~

## 8、初始化事件派发器initApplicationEventMulticaster()

~~~java

protected void initApplicationEventMulticaster() {
    //1）、获取BeanFactory
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    //判断容器中是否有applicationEventMulticaster 的这个bean ,如果有获取，没有创建
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
        //2）、从BeanFactory中获取applicationEventMulticaster的ApplicationEventMulticaster；
        this.applicationEventMulticaster =
            beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
        }
    }
    else {
        //3) 个SimpleApplicationEventMulticaster 类型的 applicationEventMulticaster
        this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        //4）、将创建的ApplicationEventMulticaster添加到BeanFactory中，以后其他组件直接自动注入
        beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
        if (logger.isDebugEnabled()) {
            logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
                         APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
                         "': using default [" + this.applicationEventMulticaster + "]");
        }
    }
}
~~~

## 9、onRefresh();留给子容器（子类）

 1、子类重写AbstractApplicationContext.onRefresh()这个方法，在容器刷新的时候可以自定义逻辑；

## 10、registerListeners();检查和注册 Listener

将所有项目里面的ApplicationListener注册到容器中；

~~~java

protected void registerListeners() {
    //1、从容器中拿到所有的ApplicationListener
    for (ApplicationListener<?> listener : getApplicationListeners()) {
        //2、将每个监听器添加到事件派发器中；
        getApplicationEventMulticaster().addApplicationListener(listener);
    }

     
    // 1.获取所有的ApplicationListener
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
        //2、将每个监听器添加到事件派发器中；
        getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
    }

    // earlyApplicationEvents 中保存之前的事件，
    Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (earlyEventsToProcess != null) {
        for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
           //3、派发之前步骤产生的事件；
            getApplicationEventMulticaster().multicastEvent(earlyEvent);
        }
    }
}
~~~



## 11、初始化所有剩下的单实例bean；finishBeanFactoryInitialization(beanFactory);

~~~java
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // 类型装换器
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
        beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
        beanFactory.setConversionService(
            beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

   //值解析器
    if (!beanFactory.hasEmbeddedValueResolver()) {
        beanFactory.addEmbeddedValueResolver(new StringValueResolver() {
            @Override
            public String resolveStringValue(String strVal) {
                return getEnvironment().resolvePlaceholders(strVal);
            }
        });
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
    String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
        getBean(weaverAwareName);
    }

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // Allow for caching all bean definition metadata, not expecting further changes.
    beanFactory.freezeConfiguration();

    // 初始化剩下的单实例bean
    beanFactory.preInstantiateSingletons();
}
~~~

### 11.1 beanFactory.preInstantiateSingletons()初始化剩下的单实例bean

~~~java
//preInstantiateSingletons():781, DefaultListableBeanFactory  
public void preInstantiateSingletons() throws BeansException {
    if (this.logger.isDebugEnabled()) {
        this.logger.debug("Pre-instantiating singletons in " + this);
    }
       //1）、获取容器中的所有Bean，依次进行初始化和创建对象 
   List<String> beanNames = new ArrayList<String>(this.beanDefinitionNames);
    for (String beanName : beanNames) {
        //2）、获取Bean的定义信息；RootBeanDefinition
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        //3）、Bean不是抽象的，是单实例的，不是懒加载；
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            // 3.1）、判断是否是FactoryBean；是否是实现FactoryBean接口的Bean；
            if (isFactoryBean(beanName)) {
                final FactoryBean<?> factory = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                boolean isEagerInit;
                if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                    isEagerInit = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return ((SmartFactoryBean<?>) factory).isEagerInit();
                        }
                    }, getAccessControlContext());
                }
                else {
                    isEagerInit = (factory instanceof SmartFactoryBean &&
                                   ((SmartFactoryBean<?>) factory).isEagerInit());
                }
                if (isEagerInit) {
                    getBean(beanName);
                }
            }
            else {  //3.2）、不是工厂Bean。利用getBean(beanName);创建对象
                getBean(beanName);  //详情请见  （11.1.3.2 创建bean  ->doGetBean）章节
            }
        }
    }

    // 循环所有的bean 
    for (String beanName : beanNames) {
        Object singletonInstance = getSingleton(beanName);
        //判断bean是否实现了SmartInitializingSingleton 接口如果是；就执行afterSingletonsInstantiated()；
        if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        
                        smartSingleton.afterSingletonsInstantiated();
                        return null;
                    }
                }, getAccessControlContext());
            }
            else {
                //执行实现类的afterSingletonsInstantiated()；
                smartSingleton.afterSingletonsInstantiated();
            }
        }
    }
}
~~~

### 11.1.3.2 创建bean  ->doGetBean

AbstractBeanFactory.doGetBean()方法

5）、将创建的Bean添加到缓存中singletonObjects；
ioc容器就是这些Map；很多的Map里面保存了单实例Bean，环境信息。。。。；

~~~java
protected <T> T doGetBean(
    final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
    throws BeansException {

    final String beanName = transformedBeanName(name);
    Object bean;

/**
2、先获取缓存中保存的单实例Bean。如果能获取到说明这个Bean之前被创建过（所有创建过的单实例Bean都会被缓存起来）
从private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);获取的
 **/
    Object sharedInstance = getSingleton(beanName);
    //如果没有获取到创建bean 
    if (sharedInstance != null && args == null) {
        if (logger.isDebugEnabled()) {
            if (isSingletonCurrentlyInCreation(beanName)) {
                logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
                             "' that is not fully initialized yet - a consequence of a circular reference");
            }
            else {
                logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
            }
        }
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }

    else {//3、缓存中获取不到，开始Bean的创建对象流程；
        // Fail if we're already creating this bean instance:
        // We're assumably within a circular reference.
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        //  获取父beanFatory 检查这个bean是否创建了
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // Not found -> check parent.
            String nameToLookup = originalBeanName(name);
            if (args != null) {
                return (T) parentBeanFactory.getBean(nameToLookup, args);
            }
            else {   
                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
        }

        // 4、标记当前bean已经被创建
        if (!typeCheckOnly) {
            markBeanAsCreated(beanName);
        }

        try {
            // 5、获取Bean的定义信息；
            final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            checkMergedBeanDefinition(mbd, beanName, args);

            // 6、【获取当前Bean依赖的其他Bean;如果有按照getBean()把依赖的Bean先创建出来；】
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                for (String dep : dependsOn) {
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                                        "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    registerDependentBean(dep, beanName);
                    getBean(dep); //先创建依赖的bean
                }
            }

            // 启动单实例的bean的创建流程
            if (mbd.isSingleton()) {
                //获取到单实例bean后，添加到缓存中 singletonObjects（）
      //Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);
                sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
                    @Override
                    public Object getObject() throws BeansException {
                        try {
                            //创建Bean ,见11.1.3.2.6.1
                            return createBean(beanName, mbd, args);
                        }
                        catch (BeansException ex) {
                            destroySingleton(beanName);
                            throw ex;
                        }
                    }
                });
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }
// 下面部分不重要了
            else if (mbd.isPrototype()) {
                // It's a prototype -> create a new instance.
                Object prototypeInstance = null;
                try {
                    beforePrototypeCreation(beanName);
                    prototypeInstance = createBean(beanName, mbd, args);
                }
                finally {
                    afterPrototypeCreation(beanName);
                }
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            }

            else {
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
                        @Override
                        public Object getObject() throws BeansException {
                            beforePrototypeCreation(beanName);
                            try {
                                return createBean(beanName, mbd, args);
                            }
                            finally {
                                afterPrototypeCreation(beanName);
                            }
                        }
                    });
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                }
                catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                                                    "Scope '" + scopeName + "' is not active for the current thread; consider " +
                                                    "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                                                    ex);
                }
            }
        }
        catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }
    }

    // Check if required type matches the type of the actual bean instance.
    if (requiredType != null && bean != null && !requiredType.isInstance(bean)) {
        try {
            return getTypeConverter().convertIfNecessary(bean, requiredType);
        }
        catch (TypeMismatchException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to convert bean '" + name + "' to required type '" +
                             ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }
    return (T) bean;
}
~~~

### 11.1.3.2.6.1 创建单实例bean

AbstractAutowireCapableBeanFactory.createBean() 这个类中

~~~java

protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
    if (logger.isDebugEnabled()) {
        logger.debug("Creating instance of bean '" + beanName + "'");
    }
    RootBeanDefinition mbdToUse = mbd; //bean定义信息

    // 解析bean的类型
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    // Prepare method overrides.
    try {
        mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                                               beanName, "Validation of method overrides failed", ex);
    }

    try {
        //让BeanPostProcessor先拦截返回代理对象；
        /** 判断是否为：InstantiationAwareBeanPostProcessor 的，如果是执行前后置方法
        【InstantiationAwareBeanPostProcessor】：提前执行；
		先触发：postProcessBeforeInstantiation()；
		如果有返回值：再触发postProcessAfterInitialization()；
        **/
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {  //如果 有对象返回
            return bean;
        }
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                                        "BeanPostProcessor before instantiation of bean failed", ex);
    }
   // 没有对象  创建一个bean
    Object beanInstance = doCreateBean(beanName, mbdToUse, args);
    if (logger.isDebugEnabled()) {
        logger.debug("Finished creating instance of bean '" + beanName + "'");
    }
    //返回创建的bean 
    return beanInstance;
}
~~~



### 1、doCreateBean()创建一个bean 

   后置处理器：MergedBeanDefinitionPostProcessor ，bean实例化之后调用

~~~java
//doCreateBean():508, AbstractAutowireCapableBeanFactory 类中
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
    throws BeanCreationException {

    // bean的包装
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
        // 1）、【创建Bean实例】利用工厂方法或者对象的构造器创建出Bean实例；
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
    Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
    mbd.resolvedTargetType = beanType;

    // Allow post-processors to modify the merged bean definition.
    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                //调用MergedBeanDefinitionPostProcessor的postProcessMergedBeanDefinition(mbd, beanType, beanName);
                //判断是否为：MergedBeanDefinitionPostProcessor 类型的，如果是，调用方法
                //MergedBeanDefinitionPostProcessor 后置处理器是在bean实例换之后调用的
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                                "Post-processing of merged bean definition failed", ex);
            }
            mbd.postProcessed = true;
        }
    }

    //判断bean 是否为单实例的，如果是单实例的添加到缓存中
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                                      isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        if (logger.isDebugEnabled()) {
            logger.debug("Eagerly caching bean '" + beanName +
                         "' to allow for resolving potential circular references");
        }
        //添加bean到缓存中
        addSingletonFactory(beanName, new ObjectFactory<Object>() {
            @Override
            public Object getObject() throws BeansException {
                return getEarlyBeanReference(beanName, mbd, bean);
            }
        });
    }

    // Initialize the bean instance.
    Object exposedObject = bean;
    try {
        //为bean 赋值,见1.1部分
        populateBean(beanName, mbd, instanceWrapper);
        if (exposedObject != null) {
            // 4）、【Bean初始化】
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        }
    }
    catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
            throw (BeanCreationException) ex;
        }
        else {
            throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
        }
    }

    if (earlySingletonExposure) {
        //获取单实例bean,此时已经创建好了
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                if (!actualDependentBeans.isEmpty()) {
                    throw new BeanCurrentlyInCreationException(beanName,
                                                               "Bean with name '" + beanName + "' has been injected into other beans [" +
                                                               StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                                                               "] in its raw version as part of a circular reference, but has eventually been " +
                                                               "wrapped. This means that said other beans do not use the final version of the " +
                                                               "bean. This is often the result of over-eager type matching - consider using " +
                                                               "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                }
            }
        }
    }

    //  5）、注册实现了DisposableBean 接口Bean的销毁方法；只是注册没有去执行，容器关闭之后才去调用的
    try {
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }

    return exposedObject;
}
~~~

### 1.1 populateBean()创建bean后属性赋值

~~~java
//populateBean():1204, AbstractAutowireCapableBeanFactory 
protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
    PropertyValues pvs = mbd.getPropertyValues();

    if (bw == null) {
        if (!pvs.isEmpty()) {
            throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
        }
        else {
            // Skip property population phase for null instance.
            return;
        }
    }

    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.
    boolean continueWithPropertyPopulation = true;

    //赋值之前：
    /* 1）、拿到InstantiationAwareBeanPostProcessor后置处理器；
        执行处理器的postProcessAfterInstantiation()； 方法*/
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                //执行
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }
    }

    if (!continueWithPropertyPopulation) {
        return;
    }

    if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
        mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

        // autowire  按name注入
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }

        // autowire  按类型注入
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }

        pvs = newPvs;
    }

    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);
/*2）、拿到InstantiationAwareBeanPostProcessor后置处理器；
	执行 postProcessPropertyValues()； 获取到属性的值，此时还未赋值*/
    if (hasInstAwareBpps || needsDepCheck) {
        PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
        if (hasInstAwareBpps) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                    if (pvs == null) {
                        return;
                    }
                }
            }
        }
        if (needsDepCheck) {
            checkDependencies(beanName, mbd, filteredPds, pvs);
        }
    }
//=====赋值之前：===  
//3）、应用Bean属性的值；为属性利用setter方法等进行赋值；
    applyPropertyValues(beanName, mbd, bw, pvs);
}
~~~

### 1.2 、Bean初始化 initializeBean

- initializeBean():1605, AbstractAutowireCapableBeanFactory  

~~~java
//初始化
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
        //1）、【执行Aware接口方法】invokeAwareMethods(beanName, bean);执行xxxAware接口的方法
		//判断是否实现了 BeanNameAware\BeanClassLoaderAware\BeanFactoryAware 这些接口的bean，如果是执行相应的方法
        invokeAwareMethods(beanName, bean);
    }

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        //2）、【执行后置处理器BeanPostProcessor初始化之前】 执行所有的 BeanPostProcessor.postProcessBeforeInitialization（）;
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        //3）、【执行初始化方法】
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
    }
	//4）、【执行后置处理器初始化之后】	执行所有的beanProcessor.postProcessAfterInitialization(result, beanName);方法
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }
    return wrappedBean;
}
~~~

#### 1.2.1  执行bean的初始化方法init

1）、是否是InitializingBean接口的实现；执行接口规定的初始化；

2）、是否自定义初始化方法；通过注解的方式添加了initMethod方法的，

​    例如： @Bean(initMethod="init",destroyMethod="detory")

~~~java
//invokeInitMethods():1667, AbstractAutowireCapableBeanFactory  
protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd)
    throws Throwable {
//1）、是否是InitializingBean接口的实现；执行接口规定的初始化 ,执行afterPropertiesSet()这个方法；
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (logger.isDebugEnabled()) {
            logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }
        if (System.getSecurityManager() != null) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        ((InitializingBean) bean).afterPropertiesSet();
                        return null;
                    }
                }, getAccessControlContext());
            }
            catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        }
        else {
            //执行实现InitializingBean的afterPropertiesSet方法
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

    if (mbd != null) {
        //执行通过注解自定义的initMethod 方法
        String initMethodName = mbd.getInitMethodName();
        if (initMethodName != null && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
~~~

## 12、完成BeanFactory的初始化创建工作；IOC容器就创建完成；刷新

~~~java
protected void finishRefresh() {
    //1. 初始化生命周期有关的后置处理器，BeanFactory创建完成后刷新相关的工作
    //默认从容器中找是否有lifecycleProcessor的组件【LifecycleProcessor】；如果没有new DefaultLifecycleProcessor();加入到容器；
    initLifecycleProcessor();
 		
    //2. 拿到前面定义的生命周期处理器（LifecycleProcessor）；回调onRefresh()；
    getLifecycleProcessor().onRefresh();

    //3. 发布容器刷新完成事件；
    publishEvent(new ContextRefreshedEvent(this));

    //4.暴露
    LiveBeansView.registerApplicationContext(this);
}
~~~

# 二、Spring 启动流程的总结

======总结===========
1）、Spring容器在启动的时候，先会保存所有注册进来的Bean的定义信息；
   1）、xml注册bean；<bean>
   2）、注解注册Bean；@Service、@Component、@Bean、xxx
2）、Spring容器会合适的时机创建这些Bean
   1）、用到这个bean的时候；利用getBean创建bean；创建好以后保存在容器中；
   2）、统一创建剩下所有的bean的时候；finishBeanFactoryInitialization()；
3）、后置处理器；BeanPostProcessor
   1）、每一个bean创建完成，都会使用各种后置处理器进行处理；来增强bean的功能；
      AutowiredAnnotationBeanPostProcessor:处理自动注入
      AnnotationAwareAspectJAutoProxyCreator:来做AOP功能；
      xxx....
      增强的功能注解：
      AsyncAnnotationBeanPostProcessor
      ....
4）、事件驱动模型；
   ApplicationListener；事件监听；
   ApplicationEventMulticaster；事件派发：