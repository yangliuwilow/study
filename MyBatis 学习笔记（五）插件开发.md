# MyBatis 学习笔记（五）插件Interceptor原理和开发

## 一、原理

 在四大对象（**Executor，StatementHandler，ParameterHandler，ResultSetHandler**）创建的时候
	   1、每个创建出来的对象不是直接返回的，而是创建完后再执行这个方法
	  		interceptorChain.pluginAll(parameterHandler);
	   2、获取到所有的Interceptor（拦截器）（插件需要实现的接口）；
	   		调用interceptor.plugin(target);返回target包装后的对象
	   3、插件机制，我们可以使用插件为目标对象创建一个代理对象；AOP（面向切面）
	   		我们的插件可以为四大对象创建出代理对象；
	  		代理对象就可以拦截到四大对象的每一个执行；

####  mybatis四大对象创建过程都执行了pluginAll（）方法

~~~java
//Configuration 中
public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
    parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
}

public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
                                            ResultHandler resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
    resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
    return resultSetHandler;
}

public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
}

public Executor newExecutor(Transaction transaction) {
    return newExecutor(transaction, defaultExecutorType);
}

public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
        executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
        executor = new ReuseExecutor(this, transaction);
    } else {
        executor = new SimpleExecutor(this, transaction);
    }
    if (cacheEnabled) {
        executor = new CachingExecutor(executor);
    }
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
}
~~~

**拦截器链InterceptorChain 执行**

```java
public class InterceptorChain {

  private final List<Interceptor> interceptors = new ArrayList<Interceptor>();
  //在拦截器链中获取所有的拦截器，执行拦截器的plugin()
  public Object pluginAll(Object target) {
    for (Interceptor interceptor : interceptors) {
      target = interceptor.plugin(target);
    }
    return target;
  }

  public void addInterceptor(Interceptor interceptor) {
    interceptors.add(interceptor);
  }
  
  public List<Interceptor> getInterceptors() {
    return Collections.unmodifiableList(interceptors);
  }

}
```



## 二、创建拦截器

###  1、编写Interceptor的实现类

~~~java
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.Properties;


/**
 * 完成插件签名：
 * 告诉MyBatis当前插件用来拦截哪个对象的哪个方法
 * type  指四大对象拦截哪个对象，
 * method ： 代表拦截哪个方法  ,在StatementHandler 中查看，需要拦截的方法
 * args   ：代表参数
 *
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "parameterize", args = {java.sql.Statement.class})
        })
public class ExamplePlugin implements Interceptor {
    /**
     * intercept：拦截
     * 拦截目标对象的目标方法的执行
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    public Object intercept(Invocation invocation) throws Throwable {

        System.out.println("intercept...intercept:"+invocation.getMethod());
        //动态的改变一下sql运行的参数：以前1号员工，实际从数据库查询3号员工
        Object target = invocation.getTarget();
        System.out.println("当前拦截到的对象："+target);
        //拿到：StatementHandler==>ParameterHandler===>parameterObject
        //拿到target的元数据
        MetaObject metaObject = SystemMetaObject.forObject(target);
        Object value = metaObject.getValue("parameterHandler.parameterObject");

        System.out.println("sql语句用的参数是："+value);
        //修改完sql语句要用的参数
        metaObject.setValue("parameterHandler.parameterObject", 3);

        //metaObject.getValue()可以取到拦截目标对象StatementHandler 里面的属性；在BaseStatementHandler里看StatementHandler所有可以取到属性
        Object mappedStatement = metaObject.getValue("parameterHandler.mappedStatement");
         
        System.out.println("mappedStatement："+mappedStatement);
        //执行目标方法
        Object proceed = invocation.proceed();
        //返回执行后的返回值
        return proceed;
    }

    /**
     * plugin：
     * 包装目标对象的：包装：为目标对象创建一个代理对象
     */
    public Object plugin(Object target) {
        //我们可以借助Plugin的wrap方法来使用当前Interceptor包装我们目标对象
        System.out.println("ExamplePlugin...plugin:mybatis将要包装的对象"+target);
        Object wrap = Plugin.wrap(target, this);
        //返回为当前target创建的动态代理
        return wrap;
    }

    /**
     * setProperties：
     * 		将插件注册时 的property属性设置进来
     */
    public void setProperties(Properties properties) {
        System.out.println("插件配置的信息："+properties);
    }
}

~~~



###  2、将写好的插件注册到全局配置文件中	

~~~xml
<!-- mybatis-config.xml  注册插件-->
<plugins>
    <plugin interceptor="com.willow.interceptor.ExamplePlugin">
        <property name="someProperty" value="100"/>
    </plugin>
</plugins>
~~~



**为目标对象创建一个代理对象**

~~~java
//Plugin
public static Object wrap(Object target, Interceptor interceptor) {
    Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor); //注解的签名
    Class<?> type = target.getClass(); //目标类型
    Class<?>[] interfaces = getAllInterfaces(type, signatureMap); //拦截的目标接口
    if (interfaces.length > 0) { //如果有拦截的接口，返回目标对象的代理对象
        return Proxy.newProxyInstance(
            type.getClassLoader(),
            interfaces,
            new Plugin(target, interceptor, signatureMap));//返回目标对象的代理对象
    }
    return target; //如果没有，直接返回
}
~~~



### 	