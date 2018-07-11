# MyBatis 学习笔记（八）自定义类型转换器TypeHanlder

使用场景：mybatis在预处理语句（PreparedStatement）中设置一个参数时，或者从结果集（ResultSet）中取出一个值时，都会用到TypeHandler。它的作用就是将java类型（javaType）转化为jdbc类型（jdbcType），或者将jdbc类型（jdbcType）转化为java类型（javaType）。  

~~~java
public interface TypeHandler<T> {

    //设置参数  
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    //按列名，拿到值封装成javabean对象
    T getResult(ResultSet rs, String columnName) throws SQLException;
    //按索引，拿到值封装成javabean对象
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    //存储过程拿到值，封装
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
~~~

mybatis创建TypeHandlerRegistry对象初始内置TypeHandler： 

~~~java
public TypeHandlerRegistry() {
    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());
    register(JdbcType.BOOLEAN, new BooleanTypeHandler());
    register(JdbcType.BIT, new BooleanTypeHandler());
    //.....
}
~~~

### 一、创建自定义类型转换器

~~~java
import com.willow.dao.model.DelFlagEnum;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


//实现TypeHandler 接口或者继承 BaseTypeHandler
public class MyTypeHanlder implements TypeHandler<DelFlagEnum> {

    //设置参数
    @Override
    public void setParameter(PreparedStatement ps, int i, DelFlagEnum parameter, JdbcType jdbcType) throws SQLException {
        System.out.println("要保存的状态码："+parameter.getStatus());
        ps.setString(i, parameter.getStatus().toString());
    }

    //按列名，拿到值封装成javabean对象
    @Override
    public DelFlagEnum getResult(ResultSet rs, String columnName) throws SQLException {
        //需要根据从数据库中拿到的枚举的状态码返回一个枚举对象
        int status = rs.getInt(columnName);
        System.out.println("从数据库中获取的状态码："+status);
        DelFlagEnum delFlagEnum = DelFlagEnum.getNameByStatus(status);
        return delFlagEnum;
    }
    //按索引，拿到值封装成javabean对象
    @Override
    public DelFlagEnum getResult(ResultSet rs, int columnIndex) throws SQLException {
        int status = rs.getInt(columnIndex);
        System.out.println("从数据库中获取的状态码："+status);
        DelFlagEnum delFlagEnum = DelFlagEnum.getNameByStatus(status);
        return delFlagEnum;
    }
    //存储过程拿到值，封装
    @Override
    public DelFlagEnum getResult(CallableStatement cs, int columnIndex) throws SQLException {
        int status = cs.getInt(columnIndex);
        System.out.println("从数据库中获取的状态码："+status);
        DelFlagEnum delFlagEnum = DelFlagEnum.getNameByStatus(status);
        return delFlagEnum;
    }
}
~~~

### 二、创建javaBean,定义字段为enum类型

~~~java
public class SysDept implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer deptId;

    /** 部门名称 */
    private String name;

    /** 排序 */
    private Integer orderNum;

    /** 创建时间 */
    private Date createTime;

    /** 修改时间 */
    private Date updateTime;

    /** 是否删除  -1：已删除  0：正常 */
    private DelFlagEnum delFlag=DelFlagEnum.USABLE;

    private Integer parentId;
~~~

### 三、创建枚举

~~~java

public enum DelFlagEnum {

    DELETE(0,"删除"),USABLE(1,"正常");

    private Integer status;
    private String name;
    //get  set ....
    DelFlagEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }
    public static DelFlagEnum getNameByStatus(Integer status){
        switch (status) {
            case (0):
                 return DELETE;
            case(1):
                return  USABLE;
            default:
                return  USABLE;
        }

    }
}
~~~

### 四、配置文件xml中注册类型转换器

~~~xml
<typeHandlers>
    <!--1、配置我们自定义的TypeHandler  -->
    <typeHandler handler="com.willow.typeHanlder.MyTypeHanlder" javaType="com.willow.dao.model.DelFlagEnum"/>
        <!--2、也可以在处理某个字段的时候告诉MyBatis用什么类型处理器
                保存：#{empStatus,typeHandler=xxxx}
查询：
                    <resultMap type="com.atguigu.mybatis.bean.Employee" id="MyEmp">
                         <id column="id" property="id"/>
                         <result column="empStatus" property="empStatus" typeHandler=""/>
                     </resultMap>
                注意：如果在参数位置修改TypeHandler，应该保证保存数据和查询数据用的TypeHandler是一样的。
          -->
    </typeHandlers>
~~~



### 五、测试结果

~~~java
@Test
public void saveTypeHadler() {
    SqlSession session = getSqlSessionFactory().openSession();
    try {
        DeptMapper deptMapper = (DeptMapper) session.getMapper(DeptMapper.class);
        SysDept dept=new SysDept(UUID.randomUUID().toString().substring(1,6), 1, new Date(),  new Date(), 1);
        //deptMapper.saveSysDept(dept);
        SysDept de= deptMapper.selectById(410015);
        System.out.println(de.getDelFlag().getName());
        session.commit();
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        session.close();
    }
}
~~~







