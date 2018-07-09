#  MyBatis 学习笔记（七）批量插入ExecutorType.BATCH效率对比

### 一、在mybatis中ExecutorType的使用


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

