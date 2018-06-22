## Spring IOC  扩展原理之BeanFactoryPostProcessor和事件监听ApplicationListener



BeanPostProcessor：bean后置处理器，bean创建对象初始化前后进行拦截工作的

1、BeanFactoryPostProcessor：beanFactory的后置处理器；
*     在BeanFactory标准初始化之后调用，来定制和修改BeanFactory的内容；
*     所有的bean定义已经保存加载到beanFactory，但是bean的实例还未创建

## 一、BeanFactoryPostProcessor原理:

创建自定义的BeanFactoryPostProcessor,断点到方法上，查看执行的流程

```java
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("MyBeanFactoryPostProcessor.....");
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (int i = 0; i <beanDefinitionNames.length ; i++) {
            System.out.println("bean:"+beanDefinitionNames[i]);
        }
    }
}
```

### 1)、ioc容器创建对象

### 2)、invokeBeanFactoryPostProcessors(beanFactory);

如何找到所有的BeanFactoryPostProcessor并执行他们的方法；

1）、直接在BeanFactory中找到所有类型是BeanFactoryPostProcessor的组件，并执行他们的方法

PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

2）、在初始化创建其他组件前面执行



## 二、 BeanDefinitionRegistryPostProcessor 原理

 BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor           postProcessBeanDefinitionRegistry();

​     在所有bean定义信息将要被加载，bean实例还未创建的；在BeanFactoryPostProcessor接口方法之前执行

创建自定义的BeanDefinitionRegistryPostProcessor 查看执行过程

~~~java
import com.willow.bean.Color;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    //在postProcessBeanDefinitionRegistry() 之后执行
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("postProcessBeanFactory...bean的数量："+beanFactory.getBeanDefinitionCount());
    }

    //BeanDefinitionRegistry Bean定义信息的保存中心，以后BeanFactory就是按照BeanDefinitionRegistry里面保存的每一个bean定义信息创建bean实例；
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        System.out.println("postProcessBeanDefinitionRegistry...bean的数量："+registry.getBeanDefinitionCount());
        //RootBeanDefinition beanDefinition = new RootBeanDefinition(Blue.class);
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(Color.class).getBeanDefinition();
        registry.registerBeanDefinition("hello", beanDefinition);
    }
}
~~~



结论：

-  优先于BeanFactoryPostProcessor执行；

*     利用BeanDefinitionRegistryPostProcessor给容器中再额外添加一些组件；



### 2.1 BeanDefinitionRegistryPostProcessor 执行过程

1）、ioc创建对象

2）、refresh()->PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory);

3）、从容器中获取到所有的BeanDefinitionRegistryPostProcessor组件,并排序

​       1、依次触发所有的postProcessBeanDefinitionRegistry()方法

4）、再来从容器中找到BeanFactoryPostProcessor组件；然后依次触发postProcessBeanFactory()方法

```java
//先执行 BeanDefinitionRegistryPostProcessor
//PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory); 这个方法中
sortPostProcessors(currentRegistryProcessors, beanFactory);
registryProcessors.addAll(currentRegistryProcessors);
invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
 
//执行所有的BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry()方法
private static void invokeBeanDefinitionRegistryPostProcessors(
    Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

    for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
        postProcessor.postProcessBeanDefinitionRegistry(registry);
    }
}
//后执行BeanFactoryPostProcessor
invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
private static void invokeBeanFactoryPostProcessors(
    Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {
    for (BeanFactoryPostProcessor postProcessor : postProcessors) {
        postProcessor.postProcessBeanFactory(beanFactory);
    }
}
```

 

## 三、事件监听ApplicationListener



ApplicationListener：监听容器中发布的事件。事件驱动模型开发；

public interface ApplicationListener<E extends ApplicationEvent>

监听 ApplicationEvent 及其下面的子事件；

### 3.1创建监听器步骤：

1）、写一个监听器（ApplicationListener实现类）来监听某个事件（ApplicationEvent及其子类）

@EventListener;

原理：使用EventListenerMethodProcessor处理器来解析方法上的@EventListener；

2）、把监听器加入到容器；

3）、只要容器中有相关事件的发布，我们就能监听到这个事件；

​              ContextRefreshedEvent：容器刷新完成（所有bean都完全创建）会发布这个事件；

​              ContextClosedEvent：关闭容器会发布这个事件；

4）、发布一个事件：applicationContext.publishEvent()；

```java
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationListener implements ApplicationListener<ApplicationEvent> {
    //当容器中发布此事件以后，方法触发
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("收到事件："+event);
    }
}
```

测试类：

```java
@Test
public void test01() {
    AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(ExtConfig.class);
    ApplicationEvent applicationEvent=new ApplicationEvent(new String("我发布的一个事件")){};
    annotationConfigApplicationContext.publishEvent(applicationEvent );
    annotationConfigApplicationContext.close();
}
```



### 3.2监听器原理：

断点在自定义创建的MyApplicationListener方法上，查看执行流程

####  3.2.1）、ContextRefreshedEvent事件：

1.    容器创建对象：refresh()；

2.    finishRefresh();容器刷新完成会发布ContextRefreshedEvent事件

   ```java
   protected void finishRefresh() {
      // Initialize lifecycle processor for this context.
      initLifecycleProcessor();
   
      // Propagate refresh to lifecycle processor first.
      getLifecycleProcessor().onRefresh();
   
      //发布ContextRefreshedEvent事件
      publishEvent(new ContextRefreshedEvent(this));
   
      // Participate in LiveBeansView MBean, if active.
      LiveBeansView.registerApplicationContext(this);
   }
   ```

3.    事件发布publishEvent(new ContextRefreshedEvent(this));

   ```java
    AbstractApplicationContext.publishEvent() 方法中
   //	getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType); 
   1）、获取事件的多播器（派发器）：getApplicationEventMulticaster()
   2）、multicastEvent派发事件：
   public void multicastEvent(final ApplicationEvent event, ResolvableType eventType) {
   		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
   		//3）、获取到所有的ApplicationListener；
   		for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
   		//1）、如果有Executor，可以支持使用Executor进行异步派发；
   			Executor executor = getTaskExecutor();
   			if (executor != null) {
   				executor.execute(new Runnable() {
   					@Override
   					public void run() {
   						invokeListener(listener, event);
   					}
   				});
   			}
   			else {
   	//2）、否则，同步的方式直接执行listener方法；invokeListener(listener, event);
   				invokeListener(listener, event);
   			}
   		}
   }
   // 拿到listener回调onApplicationEvent方法；listener.onApplicationEvent(event);
   ```

   ​    

#### 3.2.2）、自己发布事件；

#### 3.2.3）、容器关闭会发布ContextClosedEvent；

```java
 AbstractApplicationContext.doClose()  -->publishEvent(new ContextClosedEvent(this));
```



### 3.3事件多播器（派发器）初始化原理：

1）、容器创建对象：refresh();

2）、initApplicationEventMulticaster();初始化ApplicationEventMulticaster；

```java
initApplicationEventMulticaster(); //refresh();方法中
//初始化 多播器
protected void initApplicationEventMulticaster() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    //   1）、先去容器中找有没有id=“applicationEventMulticaster”的组件；
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
        this.applicationEventMulticaster =
            beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
        }
    }
    else {
        //2）、如果没有创建了一个SimpleApplicationEventMulticaster并且加入到容器中，我们就可以在其他组件要派发事件，自动注入这个applicationEventMulticaster；
        this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
        if (logger.isDebugEnabled()) {
            logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
                         APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
                         "': using default [" + this.applicationEventMulticaster + "]");
        }
}
```

​      

###  3.4 注册监听器原理：

1）、容器创建对象：refresh();

2）、注册监听器到派发器中，refresh()->registerListeners();

~~~JAVA
//AbstractApplicationContext
protected void registerListeners() {
    // Register statically specified listeners first.
    for (ApplicationListener<?> listener : getApplicationListeners()) {
        getApplicationEventMulticaster().addApplicationListener(listener);
    }

    //从容器中拿到所有的监听器，把他们注册到applicationEventMulticaster中；
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
        //将listener注册到ApplicationEventMulticaster中
        getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
    }

    // Publish early application events now that we finally have a multicaster...
    Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (earlyEventsToProcess != null) {
        for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
            getApplicationEventMulticaster().multicastEvent(earlyEvent);
        }
    }
}
~~~





### 3.4 @EventListener创建监听器：

####  3.4.1 基于注解的方式创建监听器：

~~~java
@Component
public class AnnotationListener {
    
    @EventListener(classes={ApplicationEvent.class}) //classes 监听器的类型
    public void listen(ApplicationEvent event){
        System.out.println("UserService...监听到的事件："+event);
    }
}
~~~



#### 3.4.2 @EventListener注解原理

原理：使用EventListenerMethodProcessor处理器来解析方法上的@EventListener；

再看SmartInitializingSingleton  这个类的原理即可

```java
public class EventListenerMethodProcessor implements SmartInitializingSingleton, ApplicationContextAware 
```

#### SmartInitializingSingleton 原理：->执行这个方法afterSingletonsInstantiated(); 

afterSingletonsInstantiated()；是在所有bean初始化完成之后调用的，

断点在EventListenerMethodProcessor.afterSingletonsInstantiated()方法上查看执行流程

1）、ioc容器创建对象并refresh()；

2）、finishBeanFactoryInitialization(beanFactory);初始化剩下的单实例bean；

​       1）、DefaultListableBeanFactory.preInstantiateSingletons()->先创建所有的单实例bean；getBean();

​       2）、获取所有创建好的单实例bean，判断是否是SmartInitializingSingleton类型的；如果是就调用afterSingletonsInstantiated();

~~~java
//这个类的DefaultListableBeanFactory.preInstantiateSingletons() 
for (String beanName : beanNames) {
    Object singletonInstance = getSingletone(beanName);
    //  2）、获取所有创建好的单实例bean，判断是否是SmartInitializingSingleton类型的；
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
            //调用SmartInitializingSingleton类型的这个方法afterSingletonsInstantiated()
            smartSingleton.afterSingletonsInstantiated();
        }
    }
}
~~~

















