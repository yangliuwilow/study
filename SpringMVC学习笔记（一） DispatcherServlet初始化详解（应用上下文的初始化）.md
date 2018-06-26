# SpringMVC学习笔记（一） DispatcherServlet初始化详解（应用上下文的初始化）

### 1、servlet启动时候 ，容器执行  HttpServletBean的 init()方法


~~~java
//HttpServletBean (org.springframework.web.servlet)
public final void init() throws ServletException {
    if (logger.isDebugEnabled()) {
        logger.debug("Initializing servlet '" + getServletName() + "'");
    }

    //将servlet 中封装的参数封装到PropertyValues
    PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
    if (!pvs.isEmpty()) {
        try {
            //属性编辑器控制修改我们的servletConfig属性
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
            //获取资源加载器
            ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
            //注册资源类型
            bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
            //初始化属性编辑器对象bw
            initBeanWrapper(bw);
            //设置
            bw.setPropertyValues(pvs, true);
        }
        catch (BeansException ex) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
            }
            throw ex;
        }
    }

    // 初始化SpringMVC框架的bean组件，抽象方法给我们去实现
    initServletBean();  //springMVC的FrameworkServlet类实现了这个方法

    if (logger.isDebugEnabled()) {
        logger.debug("Servlet '" + getServletName() + "' configured successfully");
    }
}
~~~



### 2、FrameworkServlet 重写了 HttpServletBean 的 initServletBean()



```java
//FrameworkServlet  重写了 HttpServletBean 的 initServletBean()
protected final void initServletBean() throws ServletException {
    getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
    if (this.logger.isInfoEnabled()) {
        this.logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
    }
    long startTime = System.currentTimeMillis();

    try {
        //创建一个web容器
        this.webApplicationContext = initWebApplicationContext();
        initFrameworkServlet();
    }
    catch (ServletException ex) {
        this.logger.error("Context initialization failed", ex);
        throw ex;
    }
    catch (RuntimeException ex) {
        this.logger.error("Context initialization failed", ex);
        throw ex;
    }

    if (this.logger.isInfoEnabled()) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        this.logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in " +
                         elapsedTime + " ms");
    }
}
```

### 2.1 初始化WebApplicationContext

~~~java
//FrameworkServlet
protected WebApplicationContext initWebApplicationContext() {
    //创建web容器
    WebApplicationContext rootContext =
        WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    WebApplicationContext wac = null;

    if (this.webApplicationContext != null) {
         // 1、在创建该Servlet注入的上下文
        wac = this.webApplicationContext;
        if (wac instanceof ConfigurableWebApplicationContext) {
            ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
            if (!cwac.isActive()) {
                
                if (cwac.getParent() == null) {
                   
                    cwac.setParent(rootContext);
                }
                //配置和刷新上下文组件
                configureAndRefreshWebApplicationContext(cwac);
            }
        }
    }
    if (wac == null) {
         //2、查找已经绑定的上下文
        wac = findWebApplicationContext();
    }
    if (wac == null) {
         //3、如果没有找到相应的上下文，并指定父亲为ContextLoaderListener
        wac = createWebApplicationContext(rootContext);
    }

    if (!this.refreshEventReceived) {
        //初始化九大组件
        onRefresh(wac);
    }

    if (this.publishContext) {
        // Publish the context as a servlet context attribute.
        String attrName = getServletContextAttributeName();
        getServletContext().setAttribute(attrName, wac);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
                              "' as ServletContext attribute with name [" + attrName + "]");
        }
    }

    return wac;
}
~~~

#### 2.1.1 刷新上下文组件 configureAndRefreshWebApplicationContext

```java
//FrameworkServlet 
protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
   if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
      
      if (this.contextId != null) {
         wac.setId(this.contextId); //上下文ID
      }
      else {
        
         wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
               ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
      }
   }

   wac.setServletContext(getServletContext());//上下文信息
   wac.setServletConfig(getServletConfig());//上下文配置信息
   wac.setNamespace(getNamespace());//上下文命名空间
   wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));
   //上下文环境信息
   ConfigurableEnvironment env = wac.getEnvironment();
   if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
   }

   postProcessWebApplicationContext(wac);
   applyInitializers(wac);//获取到所有的 ApplicationContextInitializer并执行
   wac.refresh();
}
```

#### 2.1.2初始化九大组件    onRefresh(wac);

~~~java
@Override
protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
}

protected void initStrategies(ApplicationContext context) {
        initMultipartResolver(context);//文件上传解析，如果请求类型是multipart将通过MultipartResolver进行文件上传解析  
        initLocaleResolver(context);//本地化解析  
        initThemeResolver(context);//主题解析  
        initHandlerMappings(context);//通过HandlerMapping，将请求映射到处理器  
        initHandlerAdapters(context);//通过HandlerAdapter支持多种类型的处理器  
        initHandlerExceptionResolvers(context);//如果执行过程中遇到异常，将交给HandlerExceptionResolver来解析  
        initRequestToViewNameTranslator(context);//直接解析请求到视图名  
        initViewResolvers(context);//通过viewResolver解析逻辑视图到具体视图实现  
        initFlashMapManager(context);//flash映射管理器  
	}
~~~

从如上代码我们可以看出，整个DispatcherServlet初始化的过程，具体主要做了如下两件事情： 
1、初始化Spring Web MVC使用的Web上下文，并且可能指定父容器为（ContextLoaderListener加载了根上下 
文）； 
2、初始化DispatcherServlet使用的策略，如HandlerMapping、HandlerAdapter等。

DispatcherServlet的默认配置在DispatcherServlet.properties文件中，DispatcherServlet.properties的位置在与DispatcherServlet同一个包中当Spring配置文件中没有指定配置时使用的默认策略。 

### 3 、Servlet业务请求流程

- HttpServlet (javax.servlet.http) .service()
- FrameworkServlet (org.springframework.web.servlet) .service 重写了HttpServlet 的service方法

#### ***3.1、servlet收到业务请求，执行FrameworkServlet .service ()方法***

~~~java
//FrameworkServlet 
protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
    if (HttpMethod.PATCH == httpMethod || httpMethod == null) {
        processRequest(request, response);
    }
    else {
        super.service(request, response); 
        //交给父类HttpServlet.service()去执行-->FrameworkServlet .processRequest(request, response);
    }
}
~~~



#### 3.2 、***执行具体业务FrameworkServlet.processRequest()***

~~~java
protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    Throwable failureCause = null;

    //获取LocaleContextHolder中原来保存的LocaleContext
    LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
    //获取当前的localeContext
    LocaleContext localeContext = buildLocaleContext(request);
	//获取RequestContextHolder中原来保存的RequestAttributes
    RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
    //获取当前的requestAttributes
    ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

    initContextHolders(request, localeContext, requestAttributes);

    try {
        
        //实际业务处理
        doService(request, response);
    }
    catch (ServletException ex) {
        failureCause = ex;
        throw ex;
    }
    catch (IOException ex) {
        failureCause = ex;
        throw ex;
    }
    catch (Throwable ex) {
        failureCause = ex;
        throw new NestedServletException("Request processing failed", ex);
    }

    finally {
        resetContextHolders(request, previousLocaleContext, previousAttributes);
        if (requestAttributes != null) {
            requestAttributes.requestCompleted();
        }

        if (logger.isDebugEnabled()) {
            if (failureCause != null) {
                this.logger.debug("Could not complete request", failureCause);
            }
            else {
                if (asyncManager.isConcurrentHandlingStarted()) {
                    logger.debug("Leaving response open for concurrent processing");
                }
                else {
                    this.logger.debug("Successfully completed request");
                }
            }
        }

        publishRequestHandledEvent(request, response, startTime, failureCause);
    }
}
~~~

#### 3.3 、业务处理doService()

~~~java
protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
    if (logger.isDebugEnabled()) {
        String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
        logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
                     " processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
    }

    // Keep a snapshot of the request attributes in case of an include,
    // to be able to restore the original attributes after the include.
    Map<String, Object> attributesSnapshot = null;
    if (WebUtils.isIncludeRequest(request)) {
        attributesSnapshot = new HashMap<String, Object>();
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                attributesSnapshot.put(attrName, request.getAttribute(attrName));
            }
        }
    }

    // 设置需要的组件
    request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
    request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
    request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
    request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

    FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
    if (inputFlashMap != null) {
        request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
    }
    request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
    request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

    try {
        doDispatch(request, response);
    }
    finally {
        if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
            // Restore the original attribute snapshot, in case of an include.
            if (attributesSnapshot != null) {
                restoreAttributesAfterInclude(request, attributesSnapshot);
            }
        }
    }
}
~~~



#### 3.4 、业务处理 DispatcherServlet.doDispatch()方法



~~~java
//DispatcherServlet
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;

    //安全性能
    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

    try {
        ModelAndView mv = null;    //模型视图对象
        Exception dispatchException = null;  //异常

        try {
            processedRequest = checkMultipart(request); //检查是否上传请求
            multipartRequestParsed = (processedRequest != request);

            // Determine handler for the current request.
            mappedHandler = getHandler(processedRequest); //根据request请求找到Handler
            if (mappedHandler == null || mappedHandler.getHandler() == null) {
                noHandlerFound(processedRequest, response);
                return;
            }

            // 根据Handler 找到适配器HandlerAdapter
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

            // 处理get\head 请求的LastModified
            String method = request.getMethod();
            boolean isGet = "GET".equals(method);
            if (isGet || "HEAD".equals(method)) {
                //是否使用缓存页面
                long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                if (logger.isDebugEnabled()) {
                    logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
                }
                if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                    return;
                }
            }

            //获取到所有的interceptor ，执行拦截器的 PreHandle
            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;
            }
            // HandlerAdapter 使用handle处理请求返回ModelAndView
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            //如果需要异步，直接返回
            if (asyncManager.isConcurrentHandlingStarted()) {
                return;
            }
            //当VIew为空时(比如返回类型为void)，设置默认的view
            applyDefaultViewName(processedRequest, mv);
             //获取到所有的interceptor ，执行拦截器的 postHandle()
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        }
        catch (Exception ex) {
            dispatchException = ex;
        }
        catch (Throwable err) {
            // As of 4.3, we're processing Errors thrown from handler methods as well,
            // making them available for @ExceptionHandler methods and other scenarios.
            dispatchException = new NestedServletException("Handler dispatch failed", err);
        }
        //处理返回结果，渲染页面，处理异常请求
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
    }
    catch (Exception ex) {
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
    }
    catch (Throwable err) {
        triggerAfterCompletion(processedRequest, response, mappedHandler,
                               new NestedServletException("Handler processing failed", err));
    }
    finally {
        //判断是否为异步请求
        if (asyncManager.isConcurrentHandlingStarted()) {
            // Instead of postHandle and afterCompletion
            if (mappedHandler != null) {
                mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
            }
        }
        else {
            // 删除上传的资源
            if (multipartRequestParsed) {
                cleanupMultipart(processedRequest);
            }
        }
    }
}
~~~



DispatcherServlet默认使用WebApplicationContext作为上下文，因此我们来看一下该上下文中特殊的Bean： 

1、Controller：处理器/页面控制器，做的是MVC中的C的事情，但控制逻辑转移到前端控制器了，用于对请求进行处理； 
2、HandlerMapping：请求到处理器的映射，如果映射成功返回一个HandlerExecutionChain对象（包含一个Handler处理器（页面控制器）对象、多个HandlerInterceptor拦截器）对象；如BeanNameUrlHandlerMapping将URL与Bean名字映射，映射成功的Bean就是此处的处理器； 
3、HandlerAdapter：HandlerAdapter将会把处理器包装为适配器，从而支持多种类型的处理器，即适配器设计模式的应用，从而很容易支持很多类型的处理器；如SimpleControllerHandlerAdapter将对实现了Controller接口的Bean进行适配，并且掉处理器的handleRequest方法进行功能处理； 
4、ViewResolver：ViewResolver将把逻辑视图名解析为具体的View，通过这种策略模式，很容易更换其他视图技术；如InternalResourceViewResolver将逻辑视图名映射为jsp视图； 
5、LocalResover：本地化解析，因为Spring支持国际化，因此LocalResover解析客户端的Locale信息从而方便进行国际化； 
6、ThemeResovler：主题解析，通过它来实现一个页面多套风格，即常见的类似于软件皮肤效果； 
7、MultipartResolver：文件上传解析，用于支持文件上传； 
8、HandlerExceptionResolver：处理器异常解析，可以将异常映射到相应的统一错误界面，从而显示用户友好的界面（而不是给用户看到具体的错误信息）； 
9、RequestToViewNameTranslator：当处理器没有返回逻辑视图名等相关信息时，自动将请求URL映射为逻辑视图名； 
10、FlashMapManager：用于管理FlashMap的策略接口，FlashMap用于存储一个请求的输出，当进入另一个请求时作为该请求的输入，通常用于重定向场景。





参考博客：https://blog.csdn.net/qq924862077/article/category/6348297/3

https://blog.csdn.net/u013399093/article/details/54889879 
