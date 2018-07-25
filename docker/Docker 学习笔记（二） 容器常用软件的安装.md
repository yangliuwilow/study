### 一、docker容器的安装

1、Docker 要求 CentOS 系统的内核版本高于 3.10 ，查看本页面的前提条件来验证你的CentOS 版本是否支持 Docker 。

 通过 **uname -r** 命令查看你当前的内核版本 

```shell
 $ uname -r
```

```shell
docker安装：

step 1: 安装必要的一些系统工具
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
Step 2: 添加软件源信息

sudo yum-config-manager --add-repo <http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo>

Step 3: 更新并安装 

Docker-CEsudo yum makecache fastsudo yum -y install docker-ce

Step 4: 开启Docker服务

sudo service docker start
```

安装参考：https://www.cnblogs.com/yufeng218/p/8370670.html

###  二、  Docker 常用命令

| 命令                               | 说明                                                     |
| ---------------------------------- | -------------------------------------------------------- |
| docker -verion                     | 版本                                                     |
| docker info                        | docker信息                                               |
| docker --help                      | 帮助信息                                                 |
| docker  search -s 30 tomcat        | 搜索点赞数超过30的 镜像                                  |
| docker  pull tomcat                | 等价于  docker pull tomcat:latest    默认tag为latest版本 |
| docker  iamges                     | 显示镜像                                                 |
| docker  images -q                  | 只显示镜像的ID                                           |
| docker rmi -f $(docker images -qa) | 批量删除镜像                                             |
| docker rmi  tomcat:latest          | 删除tomcat镜像                                           |
| docker rmi  -f   tomcat:latest     | -f  强制删除                                             |
| docker search centos               | 搜索镜像                                                 |
| docker rm -f$(docker ps -a -q )    | 批量删除多个容器                                         |
| docker ps -a -q \|xargs docker rm  | 批量删除多个容器                                         |
| docker start  myrunoob             | 启动已被停止的容器myrunoob                               |
| docker stop   myrunoob             | 停止运行中的容器myrunoob                                 |



### 三、docker Tomcat安装

```shell
[root@localhost ~]# docker pull tomcat
[root@localhost ~]# docker images 
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
tomcat              latest              61205f6444f9        27 hours ago        467MB
#运行
[root@localhost ~]# docker run  -d -p 8080:8080 --name mytomcat 61205f6444f9
#访问http://192.168.16.200:8080/  
#  查看所有的 容器，然后根据 CONTAINER ID 启动或者停止
[root@localhost ~]# docker ps -a   
#、 停止运行中的容器（第二次运行启动，已经映射过了）
docker stop  容器的id
#、启动容器
docker start 容器id
```

### 四、docker Mysql安装

```shell
#下载
[root@localhost ~]#  docker pull mysql:5.5
#查看下载的镜像
[root@localhost ~]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
mysql               5.5              a8a59477268d        5 weeks ago         445MB
#安装mysql  ,设置编码 和 root的登陆密码
[root@localhost ~]# docker run --name mysql  -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -d mysql:5.5 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci 


```



### 五、docker  redis安装

```shell
docker  redis安装

# docker images

# docker pull registry.docker-cn.com/library/redis                         //通过镜像下载
#或者  方法二：
[root@localhost ~]# docker pull redis
#启动
[root@localhost ~]# docker images 
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
tomcat              latest              61205f6444f9        27 hours ago        467MB
redis               latest              bfcb1f6df2db        5 weeks ago         107MB
[root@localhost ~]# docker run -d -p 6379:6379 --name  myredis bfcb1f6df2db
 #    -d 后台启动，-p暴露端口

```

### 六、docker  zookeeper安装

```shell

# docker images
# docker pull registry.docker-cn.com/library/zookeeper                         //通过镜像下载
#或者  方法二：
[root@localhost ~]# docker pull zookeeper
#启动
[root@localhost ~]# docker images
REPOSITORY                                 TAG                 IMAGE ID            CREATED             SIZE
registry.docker-cn.com/library/zookeeper   latest              2a8fecd00fba        4 days ago          146MB
[root@localhost ~]# docker run --name zookeeper -p 2181:2181  --restart always -d 2a8fecd00fba
 
#This image includes EXPOSE 2181 2888 3888 (the zookeeper client port, follower port, election port respectively), 选举等功能功能需要开启 2888 3888  端口
```

### 



### 七、docker  rabbitmq安装

```shell
#第一步搜索
[root@localhost ~]# docker search rabbitmq
#第二步拉取（选择带有management的版本，有web界面管理功能）
[root@localhost ~]#  docker pull rabbitmq:3.7.3-management
#查看镜像
[root@localhost ~]# docker images 
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
rabbitmq            3.7.3-management    2f415b0e9a6e        3 months ago        151MB
#运行
[root@localhost ~]# docker run -d -p 5672:5672 -p 15672:15672 --name myrabbitmq  2f415b0e9a6e
#访问 http://192.168.16.200:15672/#/   guest:guest 登陆
```





### 八、docker elasticsearch安装（安装2.4.6版本的）

```shell
docker  search elasticsearch    //搜索
docker pull registry.docker-cn.com/library/elastiscearch   //下载 
或者
[root@localhost ~]# docker pull elasticsearch:2.4.6  下载
#docker  images 
docker run -e ES_JAVA_OPTS="-Xms256m -Xmx256m" -d -p 9200:9200 -p 9300:9300  --name ES01  镜像Id
#默认占用2个G内存，限制内存大小，初始大小Xms256m ，最大Xmx256m


#打开浏览器访问：http://ip:9200     
#返回JSON就是安装成功
#官方文档   https://www.elastic.co/cn/products/elasticsearch
#https://www.elastic.co/guide/cn/elasticsearch/guide/current/index.html
# 基础入门-->面向文档-->
# 具体介绍文档 https://www.elastic.co/guide/cn/elasticsearch/guide/current/_retrieving_a_document.html


#指令
# get    操作获取   GET /megacorp/employee/1
# put    存储
# head   检查文档是否存在
# delete 删除
#GET /megacorp/employee/_search  搜索所有员工
#文档：https://www.elastic.co/guide/cn/elasticsearch/guide/current/_search_lite.html 

#GET /megacorp/employee/_search?q=last_name:Smith
#搜索last_name=Smith的员工
#返回的json找那个score：质量分数


#查询表达式：
https://www.elastic.co/guide/cn/elasticsearch/guide/current/_search_with_query_dsl.html
#查询last_name=smith  ；age>30的员工信息
GET /megacorp/employee/_search
{
    "query" : {
        "bool": {
            "must": {
                "match" : {
                    "last_name" : "smith" 
                }
            },
            "filter": {
                "range" : {
                    "age" : { "gt" : 30 } 
                }
            }
        }
    }
}
#全文搜索，搜索rock or climbing  ,返回_score 对应质量得分
GET /megacorp/employee/_search
{
    "query" : {
        "match" : {
            "about" : "rock climbing"
        }
    }
}
```

```shell
#docker logs  d085fe4b6e63  查看docker 指定容器的日志
```

