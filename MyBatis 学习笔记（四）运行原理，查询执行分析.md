# MyBatis 学习笔记（四）运行原理，查询执行分析

```java
public SqlSessionFactory getSqlSessionFactory() {
    String resource = "mybatis-config.xml";
    InputStream inputStream = null;
    try {
        inputStream = Resources.getResourceAsStream(resource);
    } catch (IOException e) {
        e.printStackTrace();
    }
    SqlSessionFactory sqlSessionFactory=new SqlSessionFactoryBuilder().build(inputStream);
    return sqlSessionFactory ;
}

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

### 7、CachingExecutor执行query查询获取到BoundSql

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
    if (cache != null) { //判断是否有二级缓存，有缓存执行缓存查询
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

​    先从一级缓存查询（localCache），如果有的话执行handleLocallyCachedOutputParameters

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
        //localCache  获取一级缓存
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

```java
//doQuery:58, SimpleExecutor
@Override
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  Statement stmt = null; //原生jdbc的Statement 
  try {
    Configuration configuration = ms.getConfiguration(); //当前的配置信息
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql); //Statement的处理器
     //创建Statement ,封装参数
    stmt = prepareStatement(handler, ms.getStatementLog());
    //执行查询   
    return handler.<E>query(stmt, resultHandler);
  } finally {
    closeStatement(stmt);
  }
}
```
#### 10.1、通过Configuration创建statementHandler的处理器

~~~java
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    //根据配置的statementType属性，通过路由创建statementHandler
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    // 通过拦截器链包装statementHandler返回
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
}
~~~

##### 10.1.1、在RoutingStatementHandler中初始化StatementHandler

```java
public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    //根据statementType 的类型创建不同的StatementHandler ，在xml中SQL语句中配置属性
    //例如：<select id="selectById" resultMap="BaseResultMap"  statementType="PREPARED">
    switch (ms.getStatementType()) {
        case STATEMENT: 
            delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            break;
        case PREPARED:  //默认的StatementHandler
            delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            break;
        case CALLABLE:
            delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            break;
        default:
            throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
    }
}



```

| statementType | STATEMENT，PREPARED 或 CALLABLE 的一个。这会让 MyBatis 分别使用 Statement，PreparedStatement 或 CallableStatement，默认值：PREPARED。 |
| --------------- | ------------------------------------------------------------ |
|                 |                                                              |

###### 10.1.1.1 **初始化StatementHandler** 和创建parameterHandler和resultSetHandler

~~~java
// BaseStatementHandler
protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();

    if (boundSql == null) { // issue #435, get the key before calculating the statement
        generateKeys(parameterObject);
        boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    this.boundSql = boundSql;

    //创建参数处理器parameterHandler
    this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
    //创建resultSetHandler
    this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
}
~~~

#### 10.2、通过StatementHandler创建Statement 

~~~java
private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    Connection connection = getConnection(statementLog); //获取数据库连接
    // 通过Connection 创建Statement ；connection.prepareStatement(sql);
    stmt = handler.prepare(connection, transaction.getTimeout()); //创建Statement
    handler.parameterize(stmt);//参数预编译
    return stmt;
}
~~~



##### 10.2.1 参数预编译parameterize

```java
@Override
public void parameterize(Statement statement) throws SQLException {
   //先创建PreparedStatement ，通过parameterHandler 预编译参数，
  parameterHandler.setParameters((PreparedStatement) statement);
}
//参数预编译 setParameters:92, DefaultParameterHandler
public void setParameters(PreparedStatement ps) {
    ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings != null) {
        for (int i = 0; i < parameterMappings.size(); i++) {
            ParameterMapping parameterMapping = parameterMappings.get(i);
            if (parameterMapping.getMode() != ParameterMode.OUT) {
                Object value;
                String propertyName = parameterMapping.getProperty();
                if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    value = metaObject.getValue(propertyName);
                }
                //类型处理器
                TypeHandler typeHandler = parameterMapping.getTypeHandler();
                JdbcType jdbcType = parameterMapping.getJdbcType();
                if (value == null && jdbcType == null) {
                    jdbcType = configuration.getJdbcTypeForNull();
                }
                try {
                    //调用typeHandler给PreparedStatement的SQL预编译设置参数
                    typeHandler.setParameter(ps, i + 1, value, jdbcType);
                } catch (TypeException e) {
                    throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
                } catch (SQLException e) {
                    throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
                }
            }
        }
    }
}
```



### 11、执行查询query操作返回结果集  

~~~java
//query:79, RoutingStatementHandler
@Override
public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    return delegate.<E>query(statement, resultHandler);
}
//query:64, PreparedStatementHandler
public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute(); //执行查询
    return resultSetHandler.<E> handleResultSets(ps); 
    //用结果集处理器resultSetHandler，处理返回结果
}
~~~



### 12、使用resultSetHandler处理返回结果

~~~java
//handleResultSets:171, DefaultResultSetHandler
@Override
public List<Object> handleResultSets(Statement stmt) throws SQLException {
    ErrorContext.instance().activity("handling results").object(mappedStatement.getId());

    final List<Object> multipleResults = new ArrayList<Object>();

    int resultSetCount = 0;
    //如果有返回结果，获取返回参数的 {列名columnNames，类型classNames，数据库字段类型jdbcTypes}
    ResultSetWrapper rsw = getFirstResultSet(stmt);//获取到返回的列和列类型，

    //获取到xml中配置所有的resultMap
    List<ResultMap> resultMaps = mappedStatement.getResultMaps();
    int resultMapCount = resultMaps.size();
    validateResultMapsCount(rsw, resultMapCount);
    while (rsw != null && resultMapCount > resultSetCount) {
        ResultMap resultMap = resultMaps.get(resultSetCount);
        //处理结果集
        handleResultSet(rsw, resultMap, multipleResults, null); 
        rsw = getNextResultSet(stmt);
        cleanUpAfterHandlingResultSet();
        resultSetCount++;
    }

    String[] resultSets = mappedStatement.getResultSets();
    if (resultSets != null) {
        while (rsw != null && resultSetCount < resultSets.length) {
            ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
            if (parentMapping != null) {
                String nestedResultMapId = parentMapping.getNestedResultMapId();
                ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
                handleResultSet(rsw, resultMap, null, parentMapping);
            }
            rsw = getNextResultSet(stmt);
            cleanUpAfterHandlingResultSet();
            resultSetCount++;
        }
    }

    return collapseSingleResultList(multipleResults);
}
~~~



#### 12.1、使用TypeHandler转换获取value值

执行顺序：![1530847453369](C:\Users\ADMINI~1\AppData\Local\Temp\1530847453369.png)

 

~~~java
//DefaultResultSetHandler 中，通过javabean属性映射，获取对应值，通过TypeHandler转换相应类型的值
private Object getPropertyMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
    throws SQLException {
    if (propertyMapping.getNestedQueryId() != null) {
        return getNestedQueryMappingValue(rs, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
    } else if (propertyMapping.getResultSet() != null) {
        addPendingChildRelation(rs, metaResultObject, propertyMapping);   // TODO is that OK?
        return DEFERED;
    } else {
        final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
        final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
        return typeHandler.getResult(rs, column);
    }
}
~~~



### 13、执行查询流程

![1530848277314](images\mybaits-query.png)



### 14、查询流程原理总结

​	  1、获取sqlSessionFactory对象:

​	   		解析文件的每一个信息保存在Configuration中，返回包含Configuration的DefaultSqlSession；
	   		注意：【MappedStatement】：代表一个增删改查的详细信息
	  

##### 	   2、获取sqlSession对象

​	   		返回一个DefaultSQlSession对象，包含Executor和Configuration;
	   		这一步会创建Executor对象；
	   

##### 	  3、获取接口的代理对象（MapperProxy）

​	   		getMapper，使用MapperProxyFactory创建一个MapperProxy的代理对象
	  		代理对象里面包含了，DefaultSqlSession（Executor）

##### 	   4、执行增删改查方法

​	  

##### 	   总结：

​	   	1、根据配置文件（全局，sql映射）初始化出Configuration对象
	   	2、创建一个DefaultSqlSession对象，
	   		他里面包含Configuration以及
	   		Executor（根据全局配置文件中的defaultExecutorType创建出对应的Executor）
	        3、DefaultSqlSession.getMapper（）：拿到Mapper接口对应的MapperProxy；
	        4、MapperProxy里面有（DefaultSqlSession）；
	        5、执行增删改查方法：
	   		1）、调用DefaultSqlSession的增删改查（Executor）；
	    		2）、会创建一个StatementHandler对象。
	    			（同时也会创建出ParameterHandler和ResultSetHandler）
	    		3）、调用StatementHandler预编译参数以及设置参数值;
	    			使用ParameterHandler来给sql设置参数
	    		4）、调用StatementHandler的增删改查方法；
	    		5）、ResultSetHandler封装结果
	    注意：
	    	四大对象（Executor,statementHandler,ResultSetHandler,TypeHandlerx）每个创建的时候都有一个interceptorChain.pluginAll(parameterHandler);
	   

![1530855137148](images\mybatis-query1.png)

![1530848672089](images\mybatis-exection.png)

