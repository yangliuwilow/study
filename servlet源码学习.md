# SpringMVC处理request ,response 处理整个业务逻辑请求

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
        // A context instance was injected at construction time -> use it
        wac = this.webApplicationContext;
        if (wac instanceof ConfigurableWebApplicationContext) {
            ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
            if (!cwac.isActive()) {
                // The context has not yet been refreshed -> provide services such as
                // setting the parent context, setting the application context id, etc
                if (cwac.getParent() == null) {
                    // The context instance was injected without an explicit parent -> set
                    // the root application context (if any; may be null) as the parent
                    cwac.setParent(rootContext);
                }
                //配置和刷新上下文组件
                configureAndRefreshWebApplicationContext(cwac);
            }
        }
    }
    if (wac == null) {
        // No context instance was injected at construction time -> see if one
        // has been registered in the servlet context. If one exists, it is assumed
        // that the parent context (if any) has already been set and that the
        // user has performed any initialization such as setting the context id
        wac = findWebApplicationContext();
    }
    if (wac == null) {
        // No context instance is defined for this servlet -> create a local one
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
		initMultipartResolver(context);
		initLocaleResolver(context);
		initThemeResolver(context);
		initHandlerMappings(context);
		initHandlerAdapters(context);
		initHandlerExceptionResolvers(context);
		initRequestToViewNameTranslator(context);
		initViewResolvers(context);
		initFlashMapManager(context);
	}
~~~

3 、Servlet业务请求流程

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
        //处理返回结果，渲染页面处理异常请求
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







https://blog.csdn.net/qq924862077/article/category/7184024