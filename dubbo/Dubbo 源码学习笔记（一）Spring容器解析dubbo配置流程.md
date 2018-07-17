# Dubbo 源码学习笔记（一）Spring容器解析dubbo配置流程

### 一、启动Spring 容器

~~~java
// SpringContainer
public void start() {
    String configPath = ConfigUtils.getProperty(SPRING_CONFIG);
    if (configPath == null || configPath.length() == 0) {
        configPath = DEFAULT_SPRING_CONFIG;
    }
    context = new ClassPathXmlApplicationContext(configPath.split("[,\\s]+"));
    context.start();
}
~~~

### 二、Spring refresh()->obtainFreshBeanFactory()  获取beanFactory

~~~java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    this.refreshBeanFactory();   //创建DefaultListableBeanFactory 
    ConfigurableListableBeanFactory beanFactory = this.getBeanFactory();
    if (this.logger.isDebugEnabled()) {
        this.logger.debug("Bean factory for " + this.getDisplayName() + ": " + beanFactory);
    }

    return beanFactory;
}

~~~

### 三、创建一个DefaultListableBeanFactory

~~~java
protected final void refreshBeanFactory() throws BeansException {
    if (this.hasBeanFactory()) {
        this.destroyBeans();
        this.closeBeanFactory();
    }

    try {
        // 创建DefaultListableBeanFactory
        DefaultListableBeanFactory beanFactory = this.createBeanFactory();
        beanFactory.setSerializationId(this.getId());
        this.customizeBeanFactory(beanFactory);
        this.loadBeanDefinitions(beanFactory);  //从指定的XML文件加载bean定义
        Object var2 = this.beanFactoryMonitor;
        synchronized(this.beanFactoryMonitor) {
            this.beanFactory = beanFactory;
        }
    } catch (IOException var5) {
        throw new ApplicationContextException("I/O error parsing bean definition source for " + this.getDisplayName(), var5);
    }
}
~~~

### 四、loadBeanDefinitions从指定的XML文件加载bean定义

```java
//loadBeanDefinitions:93, AbstractXmlApplicationContext
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        beanDefinitionReader.setEnvironment(this.getEnvironment());
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
        this.initBeanDefinitionReader(beanDefinitionReader);
        this.loadBeanDefinitions(beanDefinitionReader);
}
```

### 五、解析xml信息

~~~java
//registerBeanDefinitions:94, DefaultBeanDefinitionDocumentReader
public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
    this.readerContext = readerContext;
    logger.debug("Loading bean definitions");
    Element root = doc.getDocumentElement();
    doRegisterBeanDefinitions(root);
}
//1 . 执行doRegisterBeanDefinitions:142, DefaultBeanDefinitionDocumentReader 
//2 . doRegisterBeanDefinitions:142, DefaultBeanDefinitionDocumentReader
//3 . parseBeanDefinitions:172, DefaultBeanDefinitionDocumentReader

//parseCustomElement:1406, BeanDefinitionParserDelegate
public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
    String namespaceUri = getNamespaceURI(ele);  //=http://code.alibabatech.com/schema/dubbo
     // 从handlerMappings 中获取到DubboNamespaceHandler  
    NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
    if (handler == null) {
        error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
        return null;
    }
    //调用 DubboNamespaceHandler的.parse()方法
    return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
}
~~~

#### 5.1、获取xml命名空间的解析器DubboNamespaceHandler

​     读取所有jar包下的**META-INF/spring.handlers** 文件，从中加载映射关系，通过namespaceUri得到**DubboNamespaceHandler**

~~~java
//  resolve:131, DefaultNamespaceHandlerResolver
public NamespaceHandler resolve(String namespaceUri) {
    //加载指定的命名空间处理器映射。
    Map<String, Object> handlerMappings = getHandlerMappings();
    //从map获取命名空间处理器  namespaceUri= http://code.alibabatech.com/schema/dubbo
    Object handlerOrClassName = handlerMappings.get(namespaceUri);
    if (handlerOrClassName == null) {
        return null;
    }
    //如果缓存的是 NamespaceHandler 的对象直接返回，如果是Class通过反射，获取到NamespaceHandler并init
    else if (handlerOrClassName instanceof NamespaceHandler) {
        return (NamespaceHandler) handlerOrClassName;
    }
    else {
        //className=com.alibaba.dubbo.config.spring.schema.DubboNamespaceHandler
        String className = (String) handlerOrClassName;
        try {
            Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
            if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
                throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
                                             "] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
            }
            NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
            namespaceHandler.init();
            handlerMappings.put(namespaceUri, namespaceHandler);
            return namespaceHandler;
        }
        catch (ClassNotFoundException ex) {
            throw new FatalBeanException("NamespaceHandler class [" + className + "] for namespace [" +
                                         namespaceUri + "] not found", ex);
        }
        catch (LinkageError err) {
            throw new FatalBeanException("Invalid NamespaceHandler class [" + className + "] for namespace [" +
                                         namespaceUri + "]: problem with handler class file or dependent class", err);
        }
    }
}
~~~

##### 5.1.1 、从spring.handlers加载命令空间处理器映射关系

​    读取所有jar包下的**META-INF/spring.handlers** 文件，从中加载映射关系，

~~~JAVA
 //  DefaultNamespaceHandlerResolver
private Map<String, Object> getHandlerMappings() {
    if (this.handlerMappings == null) {
        synchronized (this) {
            if (this.handlerMappings == null) {
                try {
                    //从META-INF/spring.handlers 中加载映射关系，
                    Properties mappings =
                        PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Loaded NamespaceHandler mappings: " + mappings);
                    }
                    Map<String, Object> handlerMappings = new ConcurrentHashMap<String, Object>(mappings.size());
                    CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
                    this.handlerMappings = handlerMappings;
                }
                catch (IOException ex) {
                    throw new IllegalStateException(
                        "Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
                }
            }
        }
    }
    return this.handlerMappings;
}
~~~

##### 5.1.2 、DubboNamespaceHandler注册xml标签解析器

Dubbo有10个标签，每个标签会解析到对应的实体上，每个实体中的属性对应标签中的属性 

~~~java
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    public void init() {
        //注册<dubbo:application>标签解析器
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser("annotation", new DubboBeanDefinitionParser(AnnotationBean.class, true));
    }

}
~~~

### 六、调用DubboBeanDefinitionParser的parse（）

DubboBeanDefinitionParser的parse方法解析步骤

1. 初始化RootBeanDefinition
2. 获取BeanId
3. 将获取到的Bean注册到Spring
4. 将xml中配置的信息放到beandefinition的PropertyValues中

~~~java
@SuppressWarnings("unchecked")
private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required) {
    //Step1 初始化RootBeanDefinition
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setBeanClass(beanClass);
    beanDefinition.setLazyInit(false);
    //Step2 获取BeanId
    String id = element.getAttribute("id");
    //没有BeanId的一些处理步骤，把BeanName做为BeanId
    if ((id == null || id.length() == 0) && required) {
        String generatedBeanName = element.getAttribute("name");
        //BeanName没有设置则按规则生成默认BeanName
        if (generatedBeanName == null || generatedBeanName.length() == 0) {
            if (ProtocolConfig.class.equals(beanClass)) {
                //Protocol特殊处理取名为dubbo
                generatedBeanName = "dubbo";
            } else {
                //其他标签采用interface 属性做为BeanName
                generatedBeanName = element.getAttribute("interface");
            }
        }
        //经过如上步骤还为空的话，将className做为BeanName
        if (generatedBeanName == null || generatedBeanName.length() == 0) {
            generatedBeanName = beanClass.getName();
        }
        id = generatedBeanName;
        int counter = 2;
        //判断已经注册表的标签是否已经有同名的
        while (parserContext.getRegistry().containsBeanDefinition(id)) {
            //有同名的则递增
            id = generatedBeanName + (counter++);
        }
    }
    //Step3 将获取到的Bean注册到Spring
    if (id != null && id.length() > 0) {
        //BeanId 出现重复则抛异常
        if (parserContext.getRegistry().containsBeanDefinition(id)) {
            throw new IllegalStateException("Duplicate spring bean id " + id);
        }
        //将xml转换为的Bean注册到Spring的parserContext
        parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
        //为已经加载的Bean注册ID
        beanDefinition.getPropertyValues().addPropertyValue("id", id);
    }

    // 不通配置的一些特殊处理
    if (ProtocolConfig.class.equals(beanClass)) {
        //protocol 特殊处理，增加protocol属性
        for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
            BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
            PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
            if (property != null) {
                Object value = property.getValue();
                if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                    definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
                }
            }
        }
    } else if (ServiceBean.class.equals(beanClass)) {
        String className = element.getAttribute("class");
        if (className != null && className.length() > 0) {
            RootBeanDefinition classDefinition = new RootBeanDefinition();
            classDefinition.setBeanClass(ReflectUtils.forName(className));
            classDefinition.setLazyInit(false);
            parseProperties(element.getChildNodes(), classDefinition);
            beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
        }
    } else if (ProviderConfig.class.equals(beanClass)) {
        //将provider转换为service
        parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
    } else if (ConsumerConfig.class.equals(beanClass)) {
        //将consumer转换为reference
        parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
    }
    //Step4 将xml中配置的信息放到beandefinition的PropertyValues中
    Set<String> props = new HashSet<String>();
    ManagedMap parameters = null;
    for (Method setter : beanClass.getMethods()) {
        String name = setter.getName();

        if (name.length() > 3 && name.startsWith("set")
            && Modifier.isPublic(setter.getModifiers())
            && setter.getParameterTypes().length == 1) {
            //公开的set***方法
            Class<?> type = setter.getParameterTypes()[0];
            String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
            props.add(property);
            Method getter = null;
            try {
                getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
            } catch (NoSuchMethodException e) {
                try {
                    getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e2) {
                }
            }
            if (getter == null
                || !Modifier.isPublic(getter.getModifiers())
                || !type.equals(getter.getReturnType())) {
                continue;
            }
            if ("parameters".equals(property)) {
                parameters = parseParameters(element.getChildNodes(), beanDefinition);
            } else if ("methods".equals(property)) {
                parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
            } else if ("arguments".equals(property)) {
                parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
            } else {
                String value = element.getAttribute(property);
                if (value != null) {
                    value = value.trim();
                    if (value.length() > 0) {
                        if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) {
                            RegistryConfig registryConfig = new RegistryConfig();
                            registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                            beanDefinition.getPropertyValues().addPropertyValue(property, registryConfig);
                        } else if ("registry".equals(property) && value.indexOf(',') != -1) {
                            parseMultiRef("registries", value, beanDefinition, parserContext);
                        } else if ("provider".equals(property) && value.indexOf(',') != -1) {
                            parseMultiRef("providers", value, beanDefinition, parserContext);
                        } else if ("protocol".equals(property) && value.indexOf(',') != -1) {
                            parseMultiRef("protocols", value, beanDefinition, parserContext);
                        } else {
                            Object reference;
                            if (isPrimitive(type)) {
                                if ("async".equals(property) && "false".equals(value)
                                    || "timeout".equals(property) && "0".equals(value)
                                    || "delay".equals(property) && "0".equals(value)
                                    || "version".equals(property) && "0.0.0".equals(value)
                                    || "stat".equals(property) && "-1".equals(value)
                                    || "reliable".equals(property) && "false".equals(value)) {
                                    // backward compatibility for the default value in old version's xsd
                                    value = null;
                                }
                                reference = value;
                            } else if ("protocol".equals(property)
                                       && ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(value)
                                       && (!parserContext.getRegistry().containsBeanDefinition(value)
                                           || !ProtocolConfig.class.getName().equals(parserContext.getRegistry().getBeanDefinition(value).getBeanClassName()))) {
                                if ("dubbo:provider".equals(element.getTagName())) {
                                    logger.warn("Recommended replace <dubbo:provider protocol=\"" + value + "\" ... /> to <dubbo:protocol name=\"" + value + "\" ... />");
                                }
                                // backward compatibility
                                ProtocolConfig protocol = new ProtocolConfig();
                                protocol.setName(value);
                                reference = protocol;
                            } else if ("onreturn".equals(property)) {
                                int index = value.lastIndexOf(".");
                                String returnRef = value.substring(0, index);
                                String returnMethod = value.substring(index + 1);
                                reference = new RuntimeBeanReference(returnRef);
                                beanDefinition.getPropertyValues().addPropertyValue("onreturnMethod", returnMethod);
                            } else if ("onthrow".equals(property)) {
                                int index = value.lastIndexOf(".");
                                String throwRef = value.substring(0, index);
                                String throwMethod = value.substring(index + 1);
                                reference = new RuntimeBeanReference(throwRef);
                                beanDefinition.getPropertyValues().addPropertyValue("onthrowMethod", throwMethod);
                            } else {
                                if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                    BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                    if (!refBean.isSingleton()) {
                                        throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                    }
                                }
                                reference = new RuntimeBeanReference(value);
                            }
                            beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                        }
                    }
                }
            }
        }
    }
    NamedNodeMap attributes = element.getAttributes();
    int len = attributes.getLength();
    for (int i = 0; i < len; i++) {
        Node node = attributes.item(i);
        String name = node.getLocalName();
        if (!props.contains(name)) {
            if (parameters == null) {
                parameters = new ManagedMap();
            }
            String value = node.getNodeValue();
            parameters.put(name, new TypedStringValue(value, String.class));
        }
    }
    if (parameters != null) {
        beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
    }
    return beanDefinition;
}
~~~









参考：https://blog.csdn.net/architect0719/article/details/53066451