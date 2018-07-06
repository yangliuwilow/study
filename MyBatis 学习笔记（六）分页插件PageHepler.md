# MyBatis 学习笔记（六）分页插件PageHepler

源码：https://github.com/pagehelper/Mybatis-PageHelper

中文文档：https://github.com/pagehelper/Mybatis-PageHelper/blob/master/wikis/zh/HowToUse.md

**集成**

使用 PageHelper 你只需要在 classpath 中包含 [pagehelper-x.x.x.jar](http://repo1.maven.org/maven2/com/github/pagehelper/pagehelper/) 和 [jsqlparser-0.9.5.jar](http://repo1.maven.org/maven2/com/github/jsqlparser/jsqlparser/0.9.5/)。

### 1、在pom.xml中添加依赖 ：

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper</artifactId>
    <version>5.1.2</version>
</dependency>
```

### 2、在配置文件mybatis-config.xml中添加拦截器

~~~xml
<plugins>
    <!-- 分页插件pagehelper -->
    <plugin interceptor="com.github.pagehelper.PageInterceptor">
        <!-- 使用下面的方式配置参数，后面会有所有的参数介绍 -->
        <property name="param1" value="value1"/>
    </plugin>
</plugins>
~~~

#### 3、插件的使用

~~~java
@Test
public void test01() throws IOException {
    // 1、获取sqlSessionFactory对象
    SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
    // 2、获取sqlSession对象
    SqlSession openSession = sqlSessionFactory.openSession();
    try {
        EmployeeMapper mapper = openSession.getMapper(EmployeeMapper.class);
        Page<Object> page = PageHelper.startPage(5, 1);

        List<Employee> emps = mapper.getEmps();
        //传入要连续显示页码的个数   //就是jsp页码下显示的页码列表
        PageInfo<Employee> info = new PageInfo<>(emps, 5);
        for (Employee employee : emps) {
            System.out.println(employee);
        }
        /*System.out.println("当前页码："+page.getPageNum());
		 System.out.println("总记录数："+page.getTotal());
		 System.out.println("每页的记录数："+page.getPageSize());
		 System.out.println("总页码："+page.getPages());*/
        //xxx
        System.out.println("当前页码："+info.getPageNum());
        System.out.println("总记录数："+info.getTotal());
        System.out.println("每页的记录数："+info.getPageSize());
        System.out.println("总页码："+info.getPages());
        System.out.println("是否第一页："+info.isIsFirstPage());
        System.out.println("连续显示的页码：");
        int[] nums = info.getNavigatepageNums();
        for (int i = 0; i < nums.length; i++) {
            System.out.println(nums[i]);
        }
       
    } finally {
        openSession.close();
    }

}
~~~



