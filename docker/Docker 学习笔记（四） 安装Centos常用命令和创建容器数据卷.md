
### 一、Docker阿里云加速：

https://dev.aliyun.com/search.html

1、打开配置文件

```shell
[root@localhost system]# vi /usr/lib/systemd/system/docker.service 
```

2、修改配置docker.service 文件；添加镜像加速器地址

 

```shell
#添加阿里云的云加速配置：--registry-mirror=https://nyw0e37l.mirror.aliyuncs.com这个配置加速
ExecStart=/usr/bin/dockerd -H tcp://192.168.7.108:2375 -H unix://var/run/docker.sock --registry-mirror=https://nyw0e37l.mirror.aliyuncs.com

#https://dev.aliyun.com/search.html
#https://cr.console.aliyun.com/?spm=5176.1971733.0.2.NvBa2K#/accelerator
```

### 二、Docker安装Centos镜像和容器数据卷

#### 3.1、安装Centos容器

~~~shell
[root@localhost ~]# docker pull centos
[root@localhost ~]# docker run -it centos /bin/bash # i交互模式  -t 打开centos的客户端
[root@localhost ~]# docker run -d  centos  //后台运行，不进入centos命令
 #后台运行后，通过docker ps - a 查看，centos 已经关闭了，无人访问自动关闭机制，
~~~

#### 3.2、启动Centos容器

~~~shell
#启动方式二：  while循环，每2秒打印"hello willow" 一次； 在通过 docker ps - a查看，此时centos未关闭
[root@localhost ~]# docker run -name="mycentos" -d centos /bin/sh -c "while true; do echo hello willow; sleep 2;done"
#查看centos 日志，显示3条日志信息
[root@localhost ~]# docker logs -t -f --tail 3 c0c1d8c56aab
~~~

#### 3.3、退出Centos容器

~~~shell
[root@87a3fc8baf87 /]# exit                退出centos，关闭容器

按住：Ctrl+P+Q #退出centos,不关闭容器
~~~

#### 3.4、进入后台运行的Centos容器中，attach和exec

~~~shell
#查看容器的进程信息，c0c1d8c56aab 为容器ID
[root@localhost ~]# docker top c0c1d8c56aab
#进入后台运行的centos中
[root@localhost ~]# docker attach c0c1d8c56aab   

# 进入到centos终端，exec 比 attach 功能更强大
[root@localhost ~]# docker exec -t  c0c1d8c56aab  /bin/bash 

~~~

#### 3.5 、创建Centos容器数据卷，实现容器和主机之前的数据共享

​         对于一个精简的 OS，rootfs 可以很小，只需要包括最基本的命令、工具和程序库就可以了，因为底层直接用 Host 的 kernel，自己只需要提供 rootfs 就行了。由此可见对于不同的liunx发行版，kernel基本是一致的，rootfs会有差别，因此不同的发行版可以公用kernel。

创建容器数据卷，实现虚拟机和容器的虚拟机在这2个目录下数据的共享；
在虚拟机上根目录下创建一个myDataVolume目录，在docker虚拟机centos创建一个dataVolumeContainer目录

~~~shell
[root@localhost usr]# docker run -it -v /myDataVolume:/dataVolumeContainer CONTAINER_ID
~~~

#####  查看 数据卷是否绑定成功

~~~shell
[root@localhost /]# docker inspect c0c1d8c56aab  (centos_容器id)
#显示Binds 如果代表绑定成功
"HostConfig": {
            "Binds": [
                "/myDataVolume:/dataVolumeContainer"
            ],
~~~

##### 测试数据共享,在docker容器里dataVolumeContainer目录下创建文件后主机中也可以看到创建的文件

~~~shell
#在docker容器里dataVolumeContainer目录下创建文件
[root@902831d4783a dataVolumeContainer]# touch contanier.txt
[root@902831d4783a dataVolumeContainer]# ll
total 0
-rw-r--r--. 1 root root 0 Jul 25 03:07 contanier.txt
#进入主机目录myDataVolume下查看
[root@localhost myDataVolume]# ll
total 0
-rw-r--r--. 1 root root 0 Jul 24 23:07 contanier.txt

~~~

**创建容器数据卷，容器只读权限**

:ro 只读 readOnly,目录下的数据，主机可以操作，容器只能读操作，没有其他权限

~~~shell
[root@localhost usr]# docker run -it -v /myDataVolume:/dataVolumeContainer：ro c0c1d8c56aab
~~~

### 3.6、通过profile创建容器数据卷

#### 1、编写profile脚本

在宿主机下创建dokcer文件下，进入docker下创建

~~~shell
[root@localhost docker]# vi Dockerfile
~~~

Dockerfile文件内容

~~~shell
FROM centos   #依赖于centos 
VOLUME ["/dataVolumeContainer1","/dataVolumeContainer2"]  #在当前的centos创建容器数据卷
CMD echo "ok"  #输出日志
CMD /bin/bash

~~~

#### 2、build 生成镜像

~~~shell
[root@localhost docker]# docker build -f /docker/Dockerfile -t willow/centos .
# -f /docker/Dockerfile  ;-f 指定路径，为刚才创建的Dockerfile文件的路径
# -t willow/centos 为生成镜像的名称;-t: 镜像的名字及标签(tag)
~~~

##### 执行命令打印，说明成功

~~~shell
[root@localhost docker]# docker build -f /docker/Dockerfile -t willow/centos .
Sending build context to Docker daemon  2.048kB
Step 1/4 : FROM centos
 ---> 49f7960eb7e4
Step 2/4 : VOLUME ["/dataVolumeContainer1","/dataVolumeContainer2"]
 ---> Running in 0cf6e6a27fb2
Removing intermediate container 0cf6e6a27fb2
 ---> 9a4a4e295dbb
Step 3/4 : CMD echo "ok"
 ---> Running in 5517e301e439
Removing intermediate container 5517e301e439
 ---> e1b21b515701
Step 4/4 : CMD /bin/bash
 ---> Running in 27dcb2ac1c16
Removing intermediate container 27dcb2ac1c16
 ---> 5f4bd8652847
Successfully built 5f4bd8652847
Successfully tagged willow/centos:latest
[root@localhost docker]# 

~~~

##### 查看容器,生成了willow/centos 的容器

~~~shell
[root@localhost docker]# docker images 
REPOSITORY                                 TAG                 IMAGE ID            CREATED              SIZE
willow/centos                              latest              5f4bd8652847        About a minute ago   200MB
~~~

##### 启动容器,  ll命令，查看创建了2个文件dataVolumeContainer1，dataVolumeContainer2

~~~shell
[root@localhost docker]# docker  run -it  willow/centos
[root@26039bf1f851 /]#  
[root@26039bf1f851 /]# ll
total 20
lrwxrwxrwx.   1 root root    7 May 31 18:02 bin -> usr/bin
drwxr-xr-x.   2 root root    6 Jul 25 06:37 dataVolumeContainer1
drwxr-xr-x.   2 root root    6 Jul 25 06:37 dataVolumeContainer2

~~~

##### 查看运行容器信息

~~~shell
[root@localhost ~]# docker ps 
 

[root@localhost ~]# docker inspect 26039bf1f851

~~~

显示容器卷路径信息，

/var/lib/docker/volumes/8f3afffe6824ba2b6707469585f583e49556976aa643b2f7bc57c443e2592150/_data

为生成宿主机的地址

~~~shell
"Mounts": [
            {
                "Type": "volume",
                "Name": "8f3afffe6824ba2b6707469585f583e49556976aa643b2f7bc57c443e2592150",
                "Source": "/var/lib/docker/volumes/8f3afffe6824ba2b6707469585f583e49556976aa643b2f7bc57c443e2592150/_data",
                "Destination": "/dataVolumeContainer2",
                "Driver": "local",
                "Mode": "",
                "RW": true,
                "Propagation": ""
            },
            {
                "Type": "volume",
                "Name": "0e63ea119a0cb7d3ddf68ea93b5271d2718d858ac82eea5f2a3c93110368acff",
                "Source": "/var/lib/docker/volumes/0e63ea119a0cb7d3ddf68ea93b5271d2718d858ac82eea5f2a3c93110368acff/_data",
                "Destination": "/dataVolumeContainer1",
                "Driver": "local",
                "Mode": "",
                "RW": true,
                "Propagation": ""
            }
        ],

~~~

进入宿主机地址

/var/lib/docker/volumes/8f3afffe6824ba2b6707469585f583e49556976aa643b2f7bc57c443e2592150/_data下，创建了文件，dataVolumeContainer2，和dataVolumeContainer1下可以共享文件




~~~shell
[root@localhost ~]# docker pull centos
[root@localhost ~]# docker run -it centos /bin/bash # i交互模式  -t 打开centos的客户端
[root@localhost ~]# docker run -d  centos  //后台运行，不进入centos命令
 #后台运行后，通过docker ps - a 查看，centos 已经关闭了，无人访问自动关闭机制，
 
[root@87a3fc8baf87 /]# exit                退出centos，关闭

按住：Ctrl+P+Q #退出centos,不关闭


#启动方式二：  while循环，每2秒打印"hello willow" 一次； 在通过 docker ps - a查看，此时centos未关闭
[root@localhost ~]# docker run -name="mycentos" -d centos /bin/sh -c "while true; do echo hello willow; sleep 2;done"
#查看centos 日志
[root@localhost ~]# docker logs -t -f --tail 3 c0c1d8c56aab
#查看容器的进程信息
[root@localhost ~]# docker top c0c1d8c56aab
#进入后台运行的centos中
[root@localhost ~]# docker attach c0c1d8c56aab   

#对于一个精简的 OS，rootfs 可以很小，只需要包括最基本的命令、工具和程序库就可以了，因为底层直接用 Host 的 kernel，自己只需要提供 rootfs 就行了。由此可见对于不同的liunx发行版，kernel基本是一致的，rootfs会有差别，因此不同的发行版可以公用kernel。

容器和主机之前的数据共享
#创建容器数据卷，实现虚拟机和容器的虚拟机在这2个目录下数据的共享；
#在虚拟机上根目录下创建一个myDataVolume目录，在docker虚拟机centos创建一个dataVolumeContainer目录
[root@localhost usr]# docker run -it -v /myDataVolume:/dataVolumeContainer CONTAINER_ID

# 查看 数据卷是否绑定成功
[root@localhost /]# docker inspect 902831d4783a  (centos_容器id)
#显示 如果代表绑定成功，
"HostConfig": {
            "Binds": [
                "/myDataVolume:/dataVolumeContainer"
            ],


#在docker容器里dataVolumeContainer目录下创建文件
[root@902831d4783a dataVolumeContainer]# touch contanier.txt
[root@902831d4783a dataVolumeContainer]# ll
total 0
-rw-r--r--. 1 root root 0 Jul 25 03:07 contanier.txt
#进入主机目录myDataVolume下查看
[root@localhost myDataVolume]# ll
total 0
-rw-r--r--. 1 root root 0 Jul 24 23:07 contanier.txt

#创建容器数据卷，容器只读权限，:ro 只读 readOnly,目录下的数据，主机可以操作，容器只能读操作，没有其他权限
[root@localhost usr]# docker run -it -v /myDataVolume:/dataVolumeContainer：ro CONTAINER_ID

~~~



###  

