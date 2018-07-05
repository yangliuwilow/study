# MyBatis 学习笔记（四）运行原理，查询执行分析

```java
@Test
public void mybatisMapper() {
    // 1、获取sqlSessionFactory对象
    SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
    // 2、获取sqlSession对象
    SqlSession openSession = sqlSessionFactory.openSession();
    try {
        // 3、获取接口的实现类对象
        //会为接口自动的创建一个代理对象，代理对象去执行增删改查方法
        DeptMapper deptMapper = (DeptMapper) openSession.getMapper(DeptMapper.class);
        System.out.println("##########"+deptMapper);
        //4、执行查询操作
        System.out.println(deptMapper.selectById(1));
    } catch (Exception e) {
        e.printstacktrace();
    } finally {
        openSession.close();
    }
}
```

### 1、MapperProxy执行invoke查询操作

```java
//MapperProxy
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  if (Object.class.equals(method.getDeclaringClass())) { //判断是否为Object，如果是接口类型的就直接执行
    try {
      return method.invoke(this, args);
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }
  final MapperMethod mapperMethod = cachedMapperMethod(method);//得到MapperMethod 
  return mapperMethod.execute(sqlSession, args);
}
```

### 2、MapperMethod执行execute操作

```java
//execute:81, MapperMethod
public Object execute(SqlSession sqlSession, Object[] args) {//args 参数
    Object result;
    switch (command.getType()) { //判断SQL类型
        case INSERT: {  //插入
            Object param = method.convertArgsToSqlCommandParam(args);
            result = rowCountResult(sqlSession.insert(command.getName(), param));
            break;
        }
        case UPDATE: { //修改
            Object param = method.convertArgsToSqlCommandParam(args);
            result = rowCountResult(sqlSession.update(command.getName(), param));
            break;
        }
        case DELETE: { //删除
            Object param = method.convertArgsToSqlCommandParam(args);
            result = rowCountResult(sqlSession.delete(command.getName(), param));
            break;
        }
        case SELECT:  //查询
            if (method.returnsVoid() && method.hasResultHandler()) {  //0、返回空参数的执行
                executeWithResultHandler(sqlSession, args);
                result = null;
            } else if (method.returnsMany()) {  //1、返回多个参数的执行
                result = executeForMany(sqlSession, args);
            } else if (method.returnsMap()) {   //2、返回Map参数的执行
                result = executeForMap(sqlSession, args);
            } else if (method.returnsCursor()) {//3、返回游标参数的执行
                result = executeForCursor(sqlSession, args);
            } else {                            //4、返回单个对象的参数执行
                //包装参数转换，单个直接返回，多个返回Map对象
                Object param = method.convertArgsToSqlCommandParam(args);  
                result = sqlSession.selectOne(command.getName(), param);
            }
            break;
        case FLUSH:
            result = sqlSession.flushStatements();
            break;
        default:
            throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
        throw new BindingException("Mapper method '" + command.getName() 
                                   + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
}
```

### 3、ParamNameResolver执行getNamedParams()包装参数

~~~java
//包装参数转换，单个直接返回，多个参数则返回Map对象
public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    if (args == null || paramCount == 0) {
        return null;
    } else if (!hasParamAnnotation && paramCount == 1) {
        return args[names.firstKey()];
    } else {
        final Map<String, Object> param = new ParamMap<Object>();
        int i = 0;
        for (Map.Entry<Integer, String> entry : names.entrySet()) {
            param.put(entry.getValue(), args[entry.getKey()]);
           
            final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
            
            if (!names.containsValue(genericParamName)) {
                param.put(genericParamName, args[entry.getKey()]);
            }
            i++;
        }
        return param;
    }
}
~~~

### 4、执行查询 DefaultSqlSession.selectOne返回单个结果

~~~java
public <T> T selectOne(String statement, Object parameter) {
    //执行查询多个selectList
    List<T> list = this.<T>selectList(statement, parameter);
    if (list.size() == 1) {  //如果是一个结果，返回
        return list.get(0);
    } else if (list.size() > 1) { //如果是多个，返回异常
        throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
    } else {
        return null;
    }
}
~~~

### 5、DefaultSqlSession执行查询 selectList返回多个结果

```java
@Override
public <E> List<E> selectList(String statement, Object parameter) {
    return this.selectList(statement, parameter, RowBounds.DEFAULT);
}

@Override
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
        //获取MappedStatement ，MappedStatement封装了标签的详细信息
        MappedStatement ms = configuration.getMappedStatement(statement);
        //wrapCollection 参数包装
        return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
        throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
        ErrorContext.instance().reset();
    }
}
```

### 6、DefaultSqlSession执行wrapCollection 包装参数

~~~java
private Object wrapCollection(final Object object) {
    if (object instanceof Collection) {  //判断是否集合
        StrictMap<Object> map = new StrictMap<Object>();
        map.put("collection", object);
        if (object instanceof List) {
            map.put("list", object);
        }
        return map;
    } else if (object != null && object.getClass().isArray()) { //判断是否数组
        StrictMap<Object> map = new StrictMap<Object>();
        map.put("array", object); //是数组 map 中存储一个array ，保存参数
        return map;
    }
    return object;    //单个对象直接返回
}
~~~

### 7、CachingExecutor执行query查询

BoundSql：

- sql    需要执行的SQL语句
- parameterMappings     参数映射关系
- parameterObject         参数

​        

~~~java
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    //获取绑定的SQL,代表SQL语句的详细信息
    BoundSql boundSql = ms.getBoundSql(parameterObject);
    //二级缓存key
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}


@Override
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
    throws SQLException {
    Cache cache = ms.getCache();
    if (cache != null) { //判断是否有二级缓存，有缓存执行缓存
        flushCacheIfRequired(ms);
        if (ms.isUseCache() && resultHandler == null) {
            ensureNoOutParams(ms, parameterObject, boundSql);
            @SuppressWarnings("unchecked")
            List<E> list = (List<E>) tcm.getObject(cache, key);
            if (list == null) {
                list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
                tcm.putObject(cache, key, list); // issue #578 and #116
            }
            return list;
        }
    }
    //执行器：SimpleExecutor 的query方法
    return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
~~~

### 8、执行器：SimpleExecutor 的query方法

~~~java
@SuppressWarnings("unchecked")
@Override
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId()); //获取一些资源
    if (closed) {
        throw new ExecutorException("Executor was closed.");
    }
    if (queryStack == 0 && ms.isFlushCacheRequired()) {
        clearLocalCache(); //清除本地缓存
    }
    List<E> list;
    try {
        queryStack++;
        //localCache  一级缓存
        list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
        if (list != null) {  
            handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
        } else {   //如果本地缓存没有数据，执行查询queryFromDatabase
            list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
        }
    } finally {
        queryStack--;
    }
    if (queryStack == 0) {
        for (DeferredLoad deferredLoad : deferredLoads) {
            deferredLoad.load();
        }
        // issue #601
        deferredLoads.clear();
        if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
            // issue #482
            clearLocalCache();
        }
    }
    return list;
}
~~~

### 9、BaseExecutor执行queryFromDatabase() 非缓存操作

```java
//queryFromDatabase:324, BaseExecutor
private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    //1、先在本地缓存中放入一个key,value是占位符
    localCache.putObject(key, EXECUTION_PLACEHOLDER); 
    try {
        //2、执行查询
        list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
        //3、删除缓存操作
        localCache.removeObject(key);
    }
    //4、放入缓存中
    localCache.putObject(key, list);
    if (ms.getStatementType() == StatementType.CALLABLE) {
        localOutputParameterCache.putObject(key, parameter);
    }
    return list;
}
```

### 10、SimpleExecutor执行doQuery查询

MappedStatement :当前增删改查的详细信息

parameter：参数

RowBounds： 逻辑分页信息

ResultHandler：

BoundSql：SQL语句信息

```Java
//doQuery:58, SimpleExecutor
@Override
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  Statement stmt = null;
  try {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.<E>query(stmt, resultHandler);
  } finally {
    closeStatement(stmt);
  }
}
```