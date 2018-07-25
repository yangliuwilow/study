#  Docker 学习笔记（二）常用命令参数说明

### 一、Docker run 命令，创建一个新的容器并运行一个命令

##### 语法：

```
docker run [OPTIONS] IMAGE [COMMAND] [ARG...]
```

##### OPTIONS说明：

- **a stdin:** 指定标准输入输出内容类型，可选 STDIN/STDOUT/STDERR 三项；
- **-d:** 后台运行容器，并返回容器ID；
- **-i:** 以交互模式运行容器，通常与 -t 同时使用；
- **-p:** 端口映射，格式为：主机(宿主)端口:容器端口
- **-t:** 为容器重新分配一个伪输入终端，通常与 -i 同时使用；
- **--name="nginx-lb":** 为容器指定一个名称；
- **--dns 8.8.8.8:** 指定容器使用的DNS服务器，默认和宿主一致；
- **--dns-search example.com:** 指定容器DNS搜索域名，默认和宿主一致；
- **-h "mars":** 指定容器的hostname；
- **-e username="ritchie":** 设置环境变量；
- **--env-file=[]:** 从指定文件读入环境变量；
- **--cpuset="0-2" or --cpuset="0,1,2":** 绑定容器到指定CPU运行；
- **-m :**设置容器使用内存最大值；
- **--net="bridge":** 指定容器的网络连接类型，支持 bridge/host/none/container: 四种类型；
- **--link=[]:** 添加链接到另一个容器；
- **--expose=[]:** 开放一个端口或一组端口；



##### 实例

  使用docker镜像nginx:latest以后台模式启动一个容器,并将容器命名为mynginx。

```shell
  docker run --name mynginx -d nginx:latest
```

参考：http://www.runoob.com/docker/docker-command-manual.html

### 二、Docker ps 命令 ，列出容器

##### 语法：

```
docker ps [OPTIONS]
```

##### OPTIONS说明：

- **-a :**显示所有的容器，包括未运行的。
- **-f :**根据条件过滤显示的内容。
- **--format :**指定返回值的模板文件。
- **-l :**显示最近创建的容器。
- **-n :**列出最近创建的n个容器。
- **--no-trunc :**不截断输出。
- **-q :**静默模式，只显示容器编号。
- **-s :**显示总的文件大小。



### 三、Docker start/stop/restart 命令

**docker start** :启动一个或多少已经被停止的容器

**docker stop** :停止一个运行中的容器

**docker restart** :重启容器

##### 语法：

```shell
docker start [OPTIONS] CONTAINER [CONTAINER...]
docker stop [OPTIONS] CONTAINER [CONTAINER...]
docker restart [OPTIONS] CONTAINER [CONTAINER...]
```

#####  **实例**

启动已被停止的容器myrunoob

```shell
docker start myrunoob
```

停止运行中的容器myrunoob

```shell
docker stop myrunoob
```

重启容器myrunoob

```shell
docker restart myrunoob
```



### 四、Docker logs 命令，获取容器的日志

##### 语法：

```
docker logs [OPTIONS] CONTAINE
```

##### OPTIONS说明：

- **-f :** 跟踪日志输出
- **--since :**显示某个开始时间的所有日志
- **-t :** 显示时间戳
- **--tail :**仅列出最新N条容器日志

##### **实例**：

```shell
[root@localhost ~]# docker logs -t -f --tail 3 c0c1d8c56aab
# 查看容器ID:c0c1d8c56aab  ,显示时间，输出日志，只显示3条
```



### 五、Docker inspect 命令，获取容器/镜像的元数据。

##### 语法：

```
docker inspect [OPTIONS] NAME|ID [NAME|ID...]
```

##### OPTIONS说明：

- **-f :**指定返回值的模板文件。
- **-s :**显示总的文件大小。
- **--type :**为指定类型返回JSON。

##### **实例**：返回容器的JSON数据

```shell
[root@localhost ~]# docker inspect  efe64dadab42
```





### 六、Docker attach 命令 ，连接到正在运行中的容器。

```shell
docker attach [OPTIONS] CONTAINER
#进入后台运行的centos中
[root@localhost ~]# docker attach c0c1d8c56aab  
```



### 七、Docker exec 命令，在运行的容器中执行命令

##### 语法：

```shell
docker exec [OPTIONS] CONTAINER COMMAND [ARG...]
```

##### OPTIONS说明：

- **-d :**分离模式: 在后台运行
- **-i :**即使没有附加也保持STDIN 打开
- **-t :**分配一个伪终端

##### **实例**：

```shell
[root@localhost ~]# docker exec -t efe64dadab42 ls -l # 在centos中执行命令： ls -l
# 进入到centos终端，exec 比 attach 功能更强大
[root@localhost ~]# docker exec -t  efe64dadab42  /bin/bash 

# 进入到启动的tomcat目录下，
[root@localhost ~]# docker exec -it  e961a7327d43  /bin/bash
root@e961a7327d43:/usr/local/tomcat# ls

```

### 八、Docker cp命令，用于容器与主机之间的数据拷贝。

##### 语法：

```shell
docker cp [OPTIONS] CONTAINER:SRC_PATH DEST_PATH|-
```

##### OPTIONS说明：

-L :保持源目标中的链接

##### 实例

```shell
[root@localhost ~]# docker cp   efe64dadab42:/tmp/yum.log /root
#拷贝 centos中tmp目录下 yun.log 到宿主机的/root目录下
```



### 九、Docker commit 命令，从容器创建一个新的镜像。

##### 语法

```
docker commit [OPTIONS] CONTAINER [REPOSITORY[:TAG]]
```

OPTIONS说明：

- **-a :**提交的镜像作者；
- **-c :**使用Dockerfile指令来创建镜像；
- **-m :**提交时的说明文字；
- **-p :**在commit时，将容器暂停。

#####  实例：

~~~shell
[root@localhost local]# docker ps -a  #查看容器
# 根据容器ID=e961a7327d43 的tomcat创建一个新的willow/tomcat:v1镜像，
[root@localhost local]# docker commit -a "willow" -m "my tomcat"  e961a7327d43  willow/tomcat:v1
[root@localhost local]# docker images  #查看镜像，生成了一个新的tomcat镜像
#启动刚才新创建的镜像，访问http://ip:8083  
[root@localhost ~]# docker run -d -p 8083:8080 --name mytomcat8083  8b0fa81a01b3
~~~

