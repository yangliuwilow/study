#          MyBatis 学习笔记（二）运行原理SqlSession的初始化

 

### 1、获取SqlSession  

        ~~~java
@Override
public SqlSession openSession() {
      //defaultExecutorType 执行器默认类型 SIMPLE
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
}
        ~~~

> **defaultExecutorType**:配置默认的执行器。

- SIMPLE 就是普通的执行器；

- REUSE 执行器会重用预处理语句（prepared statements）；

-  BATCH 执行器将重用语句并执行批量更新。 

#### 1.1 DefaultSqlSessionFactory.openSessionFromDataSource()

~~~java
private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
       //获取环境配置
      final Environment environment = configuration.getEnvironment();
       //创建事物
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      //4、创建执行器
      final Executor executor = configuration.newExecutor(tx, execType);
      //创建DefaultSqlSession，包含Executor和configuration
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
~~~

##### 1.1.1  创建SIMPLE类型的执行器 Executor

Executor  :做增删改查操作

~~~java
//Configuration
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
        executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
        executor = new ReuseExecutor(this, transaction);
    } else {  //5、默认的SimpleExecutor执行器
        executor = new SimpleExecutor(this, transaction);
    }
    if (cacheEnabled) { //判断是否配置了二级缓存
        executor = new CachingExecutor(executor); //6、如果有创建一个CachingExecutor的执行器
    }
    //interceptorChain拦截器链
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
}

//7、拿到所有的拦截器，执行所有拦截器的plugin方法()；使用拦截器重新包装的executor ，并返回
public Object pluginAll(Object target) {
    for (Interceptor interceptor : interceptors) {
        target = interceptor.plugin(target);
    }
    return target;
}
~~~

### 2、SqlSession初始化流程 

![1530778949384](images\1530778949384.png)