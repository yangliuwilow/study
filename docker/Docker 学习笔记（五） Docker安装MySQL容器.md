# Docker 学习笔记（五） Docker安装MySQL容器



### 首先启动docker容器

```shell
[root@localhost ~]# systemctl start docker
```

### 1、搜索mysql容器命令

```shell
[root@localhost system]# docker search mysql 
```

### 2、拉取mysql镜像，默认版本

```shell
[root@localhost docker]# docker pull mysql:5.5
```

### 3、查看mysql镜像

~~~shell
[root@localhost docker]# docker images 
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
docker.io/mysql     5.5                 d404d78aa797        2 months ago        205 MB

~~~

### 4、docker 运行mysql容器，设置编码和端口

```shell
[root@localhost system]#  docker run --name mysql  -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -d mysql:5.5 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci 
```

### 5、查看mysql容器是否运行

```shell
[root@localhost docker]#  docker ps -a
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
ad5101d5dc0a        mysql:5.5           "docker-entrypoint..."   6 minutes ago       Up 6 minutes        0.0.0.0:3306->3306/tcp   mysql

```

### 6、停止mysql容器,指定容器的id（修改为自己的CONTAINERID）

```shell
[root@localhost system]# docker stop ad5101d5dc0a
```

### 7、启动已有的容器mysql

```shell
[root@localhost system]# docker start ad5101d5dc0a
```