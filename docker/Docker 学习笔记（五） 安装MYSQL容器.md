# Docker 学习笔记（五） 安装MYSQL容器



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
[root@localhost system]# docker pull mysql 
```

### 3、docker 运行mysql容器，设置编码和端口

```shell
[root@localhost system]#  docker run --name some-mysql  -p 3306:3306 -e MYSQL_ROOT_PASSWORD=my-secret-pw -d mysql:tag --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci 
```

### 4、查看mysql容器是否运行

```shell
[root@localhost system]# docker ps -a
```

### 5、停止mysql容器

```shell
[root@localhost system]# docker stop  596c36f3a06c（修改为自己的Id）
```

### 6、启动已有的容器mysql

```shell
[root@localhost system]# docker start 596c36f3a06c
```