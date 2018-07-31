# Redis 学习笔记（一） redis-3.0.4的安装

### Redis介绍：

​      REmote DIctionary Server(远程字典服务器)

​     是完全开源免费的，用C语言编写的，遵守BSD协议，是一个高性能的(key/value)分布式内存数据库，基于内存运行并支持持久化的NoSQL数据库，是当前最热门的NoSql数据库之一,也被人们称为数据结构服务器

中文文档地址：http://www.redis.cn/

官网：https://redis.io/

### 1、安装wget 命令

~~~shell
[root@localhost ~]# yum -y install wget 
~~~

### 2、下载Redis，解压，编译:

~~~shell
$ wget http://download.redis.io/releases/redis-3.0.4.tar.gz
$ tar xzf redis-3.0.4.tar.gz
$ cd redis-redis-3.0.4
$ make   #如果没有gcc提示报错，先安装gcc,能上网：yum install gcc-c++
~~~

### 3、修改redis.conf开启守护进程模式 

- `daemonize:yes`:redis采用的是单进程多线程的模式。当redis.conf中选项daemonize设置成yes时，代表开启守护进程模式。在该模式下，redis会在后台运行，并将进程pid号写入至redis.conf选项pidfile设置的文件中，此时redis将一直运行，除非手动kill该进程。
- `daemonize:no`: 当daemonize选项设置成no时，当前界面将进入redis的命令行界面，exit强制退出或者关闭连接工具(putty,xshell等)都会导致redis进程退出。

~~~shell 
[root@localhost redis-3.0.4]# vi  redis.conf

# By default Redis does not run as a daemon. Use 'yes' if you need it.
# Note that Redis will write a pid file in /var/run/redis.pid when daemonized.
daemonize yes
~~~

### 4、启动服务端

~~~shell
#1、进入安装目录
[root@localhost redis-3.0.4]# cd /usr/local/bin/
#2、启动服务，用刚才修改的redis.conf文件
[root@localhost bin]# redis-server  /redis-3.0.4/redis.conf 

~~~

### 5、打开客户端

~~~shell
# /usr/local/bin/ 目录下
[root@localhost bin]# redis-cli  -p 6379
127.0.0.1:6379> ping
PONG
127.0.0.1:6379> set name willow
OK
127.0.0.1:6379> get name
"willow"
127.0.0.1:6379> 

~~~

### 6、关闭实例并关闭服务器

~~~shell
127.0.0.1:6379> shutdown
not connected> exit
~~~

### 7、查看redis是否运行

~~~shell
[root@localhost bin]# ps -ef|grep redis
~~~

### 8、默认安装目录/usr/local/bin/文件说明

- redis-benchmark:性能测试工具，可以在自己本子运行，看看自己本子性能如何

- redis-check-aof：修复有问题的AOF文件，

- redis-check-dump：修复有问题的dump.rdb文件

- redis-cli：客户端，操作入口

- redis-sentinel：redis集群使用

- redis-server：Redis服务器启动命令

  

