# Docker 学习笔记（六） Docker无法启动的问题解决

### 1、查看liunx 版本

```shell
[root@localhost ~]# uname -r 
3.10.0-327.el7.x86_64
```

### 2、docker 安装

```shell
[root@localhost ~]# docker -v 
Docker version 1.13.1, build b2f74b2/1.13.1
```

### 3、docker 启动 

```shell
[root@localhost ~]# systemctl start docker 
Job for docker.service failed because the control process exited with error code. See "systemctl status docker.service" and "journalctl -xe" for details.
[root@localhost ~]# systemctl status docker
● docker.service - Docker Application Container Engine
   Loaded: loaded (/usr/lib/systemd/system/docker.service; disabled; vendor preset: disabled)
   Active: failed (Result: exit-code) since Sat 2019-07-27 05:15:36 EDT; 1min 21s ago
     Docs: http://docs.docker.com
  Process: 19110 ExecStart=/usr/bin/dockerd-current --add-runtime docker-runc=/usr/libexec/docker/docker-runc-current --default-runtime=docker-runc --exec-opt native.cgroupdriver=systemd --userland-proxy-path=/usr/libexec/docker/docker-proxy-current --init-path=/usr/libexec/docker/docker-init-current --seccomp-profile=/etc/docker/seccomp.json $OPTIONS $DOCKER_STORAGE_OPTIONS $DOCKER_NETWORK_OPTIONS $ADD_REGISTRY $BLOCK_REGISTRY $INSECURE_REGISTRY $REGISTRIES (code=exited, status=1/FAILURE)
 Main PID: 19110 (code=exited, status=1/FAILURE)

Jul 27 05:15:34 localhost.localdomain systemd[1]: Starting Docker Application Container Engine...
Jul 27 05:15:34 localhost.localdomain dockerd-current[19110]: time="2019-07-27T05:15:34.589441781-04:0...d"
Jul 27 05:15:34 localhost.localdomain dockerd-current[19110]: time="2019-07-27T05:15:34.607276850-04:0...5"
Jul 27 05:15:35 localhost.localdomain dockerd-current[19110]: time="2019-07-27T05:15:35.631288263-04:00"...
Jul 27 05:15:36 localhost.localdomain dockerd-current[19110]: Error starting daemon: SELinux is not su...e)
Jul 27 05:15:36 localhost.localdomain systemd[1]: docker.service: main process exited, code=exited, s...URE
Jul 27 05:15:36 localhost.localdomain systemd[1]: Failed to start Docker Application Container Engine.
Jul 27 05:15:36 localhost.localdomain systemd[1]: Unit docker.service entered failed state.
Jul 27 05:15:36 localhost.localdomain systemd[1]: docker.service failed.
Hint: Some lines were ellipsized, use -l to show in full.
[root@localhost ~]# systemctl status -l docker.service
● docker.service - Docker Application Container Engine
   Loaded: loaded (/usr/lib/systemd/system/docker.service; disabled; vendor preset: disabled)
   Active: failed (Result: exit-code) since Sat 2019-07-27 05:15:36 EDT; 6min ago
     Docs: http://docs.docker.com
  Process: 19110 ExecStart=/usr/bin/dockerd-current --add-runtime docker-runc=/usr/libexec/docker/docker-runc-current --default-runtime=docker-runc --exec-opt native.cgroupdriver=systemd --userland-proxy-path=/usr/libexec/docker/docker-proxy-current --init-path=/usr/libexec/docker/docker-init-current --seccomp-profile=/etc/docker/seccomp.json $OPTIONS $DOCKER_STORAGE_OPTIONS $DOCKER_NETWORK_OPTIONS $ADD_REGISTRY $BLOCK_REGISTRY $INSECURE_REGISTRY $REGISTRIES (code=exited, status=1/FAILURE)
 Main PID: 19110 (code=exited, status=1/FAILURE)

Jul 27 05:15:34 localhost.localdomain systemd[1]: Starting Docker Application Container Engine...
Jul 27 05:15:34 localhost.localdomain dockerd-current[19110]: time="2019-07-27T05:15:34.589441781-04:00" level=warning msg="could not change group /var/run/docker.sock to docker: group docker not found"
Jul 27 05:15:34 localhost.localdomain dockerd-current[19110]: time="2019-07-27T05:15:34.607276850-04:00" level=info msg="libcontainerd: new containerd process, pid: 19115"
Jul 27 05:15:35 localhost.localdomain dockerd-current[19110]: time="2019-07-27T05:15:35.631288263-04:00" level=warning msg="overlay2: the backing xfs filesystem is formatted without d_type support, which leads to incorrect behavior. Reformat the filesystem with ftype=1 to enable d_type support. Running without d_type support will no longer be supported in Docker 1.16."
Jul 27 05:15:36 localhost.localdomain dockerd-current[19110]: Error starting daemon: SELinux is not supported with the overlay2 graph driver on this kernel. Either boot into a newer kernel or disable selinux in docker (--selinux-enabled=false)
Jul 27 05:15:36 localhost.localdomain systemd[1]: docker.service: main process exited, code=exited, status=1/FAILURE
Jul 27 05:15:36 localhost.localdomain systemd[1]: Failed to start Docker Application Container Engine.
Jul 27 05:15:36 localhost.localdomain systemd[1]: Unit docker.service entered failed state.
Jul 27 05:15:36 localhost.localdomain systemd[1]: docker.service failed.
```

### 4、问题描述

​         意思是说：此linux的内核中的SELinux不支持 overlay2 graph driver ，解决方法有两个，要么启动一个新内核，要么就在docker里禁用selinux，--selinux-enabled=false.
重新编辑docker配置文件：
vi /etc/sysconfig/docker
改为：--selinux-enabled=false

### 5、修改docker配置文件

~~~shell
[root@localhost ~]# vi /etc/sysconfig/docker
~~~
修改前：  OPTIONS='--selinux-enabled --log-driver=journald --signature-verification=false'
修改后： OPTIONS='--selinux-enabled=false --log-driver=journald --signature-verification=false'

~~~shell
# /etc/sysconfig/docker

# Modify these options if you want to change the way the docker daemon runs
OPTIONS='--selinux-enabled=false --log-driver=journald --signature-verification=false'
if [ -z "${DOCKER_CERT_PATH}" ]; then
    DOCKER_CERT_PATH=/etc/docker
fi

# Do not add registries in this file anymore. Use /etc/containers/registries.conf
# instead. For more information reference the registries.conf(5) man page.

# Location used for temporary files, such as those created by
# docker load and build operations. Default is /var/lib/docker/tmp
# Can be overriden by setting the following environment variable.
# DOCKER_TMPDIR=/var/tmp

# Controls the /etc/cron.daily/docker-logrotate cron job status.
# To disable, uncomment the line below.
# LOGROTATE=false

# docker-latest daemon can be used by starting the docker-latest unitfile.
# To use docker-latest client, uncomment below lines
#DOCKERBINARY=/usr/bin/docker-latest
#DOCKERDBINARY=/usr/bin/dockerd-latest
#DOCKER_CONTAINERD_BINARY=/usr/bin/docker-containerd-latest
#DOCKER_CONTAINERD_SHIM_BINARY=/usr/bin/docker-containerd-shim-latest

~~~

### 6、Docker 镜像修改

~~~shell
[root@localhost docker]#   sudo vi /etc/docker/daemon.json

{
  "registry-mirrors":["https://docker.mirrors.ustc.edu.cn"]
}
#保存后刷新配置重启启动docker服务
[root@localhost docker]# sudo systemctl daemon-reload
[root@localhost docker]# sudo systemctl restart docker
#
~~~





