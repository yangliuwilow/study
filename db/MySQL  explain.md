# MySQL 中explain的使用以及性能分析



explain select *from employee  执行结果：

### 1、Id列：

多个Id 相同，执行顺序右上至下

多个Id不同，如果是子查询，id的序号会递增，id值越大优先级越高，值大的先被执行

### 2、**select_type列** 

1. **SIMPLE**:     简单的select 查询，SQL中不包含子查询或者UNION。 　

2. **PRIMARY** :   查询中包含复杂的子查询部分，最外层查询被标记为PRIMARY 

3. **SUBQUERY**:   在select 或者WHERE 列表中包含了子查询 

4. **DERIVED**:  在FROM列表中包含的子查询会被标记为DERIVED(衍生表)，MYSQL会递归执行这些子查询，把结果集放到零时表中。 　　（derived2：衍生表 2表示衍生的是id=2的表 tb1 

5. **UNION**:     如果第二个SELECT 出现在UNION之后，则被标记位UNION；如果UNION包含在FROM子句的子查询中，则外层SELECT 将被标记为DERIVED 　

6. 　**UNION RESULT**:从UNION表获取结果的select 



### 3、table列：

​           **该行数据是关于哪张表**

### 4、**type列：**

​        **访问类型 由好到差system > const > eq_ref > ref > range > index > ALL** 

　　1、**system**:表只有一条记录(等于系统表),这是const类型的特例，平时业务中不会出现。 　

​    　2、**const**:通过索引一次查到数据，该类型主要用于比较primary key 或者unique 索引，因为只匹配一行数据，所以很快;如果将主键置于WHERE语句后面，Mysql就能将该查询转换为一个常量。  　

​    　3、**eq_ref**:唯一索引扫描，对于每个索引键，表中只有一条记录与之匹配。常见于主键或者唯一索引扫描。 　　4、**ref**:非唯一索引扫描，返回匹配某个单独值得所有行，本质上是一种索引访问，它返回所有匹配某个单独值的行，就是说它可能会找到多条符合条件的数据，所以他是查找与扫描的混合体。

 　　5、**range**：只检索给定范围的行，使用一个索引来选着行。key列显示使用了哪个索引。一般在你的WHERE 语句中出现between 、< 、> 、in 等查询，这种给定范围扫描比全表扫描要好。因为他只需要开始于索引的某一点，而结束于另一点，不用扫描全部索引。

 　　6、**index**：FUll Index Scan 扫描遍历索引树(扫描全表的索引，从索引中获取数据)。

 　　7、**ALL**： 全表扫描 从磁盘中获取数据 百万级别的数据ALL类型的数据尽量优化。 

### 5、**possible_keys列:**

​       **显示可能应用在这张表的索引，一个或者多个。查询涉及到的字段若存在索引，则该索引将被列出，但不一定被查询实际使用。** 

### 6、keys列:

​         **实际使用到的索引。如果为NULL，则没有使用索引。查询中如果使用了覆盖索引，则该索引仅出现在key列表中。覆盖索引：select 后的 字段与我们建立索引的字段个数一致。**** 



### 7、**ken_len列:**

​          **表示索引中使用的字节数，可通过该列计算查询中使用的索引长度。在不损失精确性的情况下，长度越短越好。key_len 显示的值为索引字段的最大可能长度，并非实际使用长度，即key_len是根据表定义计算而得，不是通过表内检索出来的。** 

### 8、**ref列:**

​            **显示索引的哪一列被使用了**，如果可能的话，是一个常数。哪些列或常量被用于查找索引列上的值。 

### 9、rows列(每张表有多少行被优化器查询):

​            **根据表统计信息及索引选用的情况，大致估算找到所需记录需要读取的行数。** 

### 10、 Extra列：扩展属性，但是很重要的信息。

1、 **Using filesort(文件排序)**：mysql无法按照表内既定的索引顺序进行读取。

**说明：order_number是表内的一个唯一索引列，但是order by 没有使用该索引列排序，所以mysql使用不得不另起一列进行排序。**

2、**Using temporary**:Mysql使用了临时表保存中间结果，常见于排序order by 和分组查询 group by。

3、**Using index**： 表示相应的select 操作使用了覆盖索引，避免访问了表的数据行，效率不错。

如果同时出现Using where ，表明索引被用来执行索引键值的查找。

如果没有同时出现using where 表明索引用来读取数据而非执行查找动作。

4、**Using where** :查找

5、**Using join buffer** ：表示当前sql使用了连接缓存。

6、**impossible where** ：where 字句 总是false ，mysql 无法获取数据行。

7、**select tables optimized away**：

8、distinct： 



### **二、使用慢查询分析** 

在my.ini中： 

~~~xml
long_query_time=1  
log-slow-queries=d:\mysql5\logs\mysqlslow.log
~~~

把超过1秒的记录在慢查询日志中 

