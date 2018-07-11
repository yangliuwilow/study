#  MyBatis 学习笔记（七）批量插入ExecutorType.BATCH效率对比

### 一、在mybatis中ExecutorType的使用

1.Mybatis内置的ExecutorType有3种，默认的是simple，该模式下它为每个语句的执行创建一个新的预处理语句，单条提交sql；而batch模式重复使用已经预处理的语句，

并且批量执行所有更新语句，显然batch性能将更优；

 

2.但batch模式也有自己的问题，比如在Insert操作时，在事务没有提交之前，是没有办法获取到自增的id，这在某型情形下是不符合业务要求的；

 

3. 在测试中使用simple模式提交10000条数据，时间为18248 毫秒，batch模式为5023 ，性能提高70%；


~~~java

@Test
public void mybatisBatch() {
    SqlSession session = getSqlSessionFactory().openSession();
    try {
        DeptMapper deptMapper = (DeptMapper) session.getMapper(DeptMapper.class);
        long start =System.currentTimeMillis();
        for (int i = 0; i <10000 ; i++) {
            SysDept dept=new SysDept(UUID.randomUUID().toString().substring(1,6), 1, new Date(),  new Date(), 1);
            deptMapper.saveSysDept(dept);
        }
        long end =System.currentTimeMillis();
        System.out.println("耗时:"+(end-start));
        //ExecutorType.BATCH 批量耗时耗时:2134
        //单条操作耗时 耗时:8584
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        session.commit();
        session.close();
    }
}

@Test
public void saveDeptBatchOne() {
    SqlSession session = getSqlSessionFactory().openSession();
    try {
        DeptMapper deptMapper = (DeptMapper) session.getMapper(DeptMapper.class);
        long start =System.currentTimeMillis();
        List<SysDept> deptList=new ArrayList<SysDept>();
        for (int i = 0; i <100000 ; i++) {
            SysDept dept=new SysDept(UUID.randomUUID().toString().substring(1,6), 1, new Date(),  new Date(), 1);
            deptList.add(dept);
            if(i%500==0){
                deptMapper.saveDeptBatch(deptList);
                deptList.clear();
            }
        }
        deptMapper.saveDeptBatch(deptList);
        long end =System.currentTimeMillis();
        System.out.println("耗时:"+(end-start));
        //非BATCH批量耗时 耗时:938
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        session.commit();
        session.close();
    }
}

@Test
public void saveDeptBatchTwo() {
    //设置ExecutorType.BATCH原理：把SQL语句发个数据库，数据库预编译好，数据库等待需要运行的参数，接收到参数后一次运行，ExecutorType.BATCH只打印一次SQL语句，多次设置参数步骤，
    SqlSession session = getSqlSessionFactory().openSession(ExecutorType.BATCH);
    try {
        DeptMapper deptMapper = (DeptMapper) session.getMapper(DeptMapper.class);
        long start =System.currentTimeMillis();
        List<SysDept> deptList=new ArrayList<SysDept>();
        for (int i = 0; i <100000; i++) {
            SysDept dept=new SysDept(UUID.randomUUID().toString().substring(1,6), 1, new Date(),  new Date(), 1);
            deptList.add(dept);
            if(i%500==0){
                deptMapper.saveDeptBatch(deptList);
                deptList.clear();
            }
        }
        deptMapper.saveDeptBatch(deptList);
        long end =System.currentTimeMillis();
        System.out.println("耗时:"+(end-start));
        //BATCH批量耗时 耗时:822
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        session.commit();
        session.close();
    }
}

~~~

### 二、在mybatis+spring中ExecutorType的使用

1、在spring配置文件中添加批量执行的SqlSessionTemplate

~~~xml
<!--配置一个可以进行批量执行的sqlSession  -->
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
    <constructor-arg name="sqlSessionFactory" ref="sqlSessionFactoryBean"></constructor-arg>
    <constructor-arg name="executorType" value="BATCH"></constructor-arg>
</bean>
~~~

### 2、service中获取批量添加的SqlSession

~~~java
@Service
public class DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private SqlSession sqlSession;

    public List<Dept> addDept(){
        //executorType=BATCH 添加操作
        DeptMapper mapper = sqlSession.getMapper(DeptMapper.class);
        return mapper.saveDept(Dept);
    }

}

~~~

### 三、$和#的区别

```
#{}：可以获取map中的值或者pojo对象属性的值；

${}：可以获取map中的值或者pojo对象属性的值；

select * from tbl_employee where id=${id} and last_name=#{lastName}
Preparing: select * from tbl_employee where id=2 and last_name=?
	区别：
		#{}:是以预编译的形式，将参数设置到sql语句中；PreparedStatement；防止sql注入
		${}:取出的值直接拼装在sql语句中；会有安全问题；
```

