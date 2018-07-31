# Redis 学习笔记（二）  基础知识命令

### 1、Redis 数据库个数

**查看配置文件redis.conf**  

```shell
[root@localhost redis-3.0.4]# vi redis.conf 
```

设置数据库的数量，默认数据库为0，可以使用SELECT <dbid>命令在连接上指定数据库id
 databases 16

~~~shell
# Set the number of databases. The default database is DB 0, you can select
# a different one on a per-connection basis using SELECT <dbid> where
# dbid is a number between 0 and 'databases'-1
databases 16

~~~

### 2、select命令切换数据库

~~~shell
127.0.0.1:6379> select 7
OK
127.0.0.1:6379[7]> get name
(nil)
127.0.0.1:6379[7]> select 0
OK
127.0.0.1:6379> get name 
"willow"

~~~

### 3、dbsize查看当前数据库的key的数量,keys *所有key

~~~shell
127.0.0.1:6379> dbsize
(integer) 4
127.0.0.1:6379> keys *
1) "name"
2) "key:__rand_int__"
3) "mylist"
4) "counter:__rand_int__"

~~~

### 4、flushdb：清空当前库key

~~~shell
127.0.0.1:6379> FLUSHDB
OK
127.0.0.1:6379> dbsize
(integer) 0
#清空所有库的key
127.0.0.1:6379[1]> FLUSHALL

~~~





