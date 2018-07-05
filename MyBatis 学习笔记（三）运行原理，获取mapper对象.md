# MyBatis 学习笔记（三）运行原理，获取mapper代理对象

>  获取接口的代理对象（MapperProxy）
>  	getMapper，使用MapperProxyFactory创建一个MapperProxy的代理对象
> 	代理对象里面包含了，DefaultSqlSession（Executor）

```java
//3、获取接口的实现类对象
//会为接口自动的创建一个代理对象，代理对象去执行增删改查方法
DeptMapper deptMapper = (DeptMapper) session.getMapper(DeptMapper.class);
//返回org.apache.ibatis.binding.MapperProxy@610f7aa
```

### 1、从configuration获取mapper

~~~java
public <T> T getMapper(Class<T> type) {
    return configuration.<T>getMapper(type, this);
}
~~~

### 2、在mapperRegistry中获取mapper

knownMappers中保存了所有的，mapper代理对象

```java
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
  return mapperRegistry.getMapper(type, sqlSession);
}
//从knownMappers 中获取 MapperProxyFactory，这个是在初始化时候已经创建了
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
        throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
        //创建MapperProxy
        return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
        throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
}
```

### 3、MapperProxyFactory创建MapperProxy

```java
public class MapperProxy<T> implements InvocationHandler, Serializable {
}
//InvocationHandler java动态代理
```

~~~java
public T newInstance(SqlSession sqlSession) {
    final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
}
~~~

### 4、创建代理对象MapperProxy

~~~java
//7、创建代理对象MapperProxy
protected T newInstance(MapperProxy<T> mapperProxy) {
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
    //mapperInterface=interface com.willow.dao.mapper.DeptMapper
}
~~~

### 5、getMapper运行流程图

![1530780707220](images\mybatis-mapper.png)