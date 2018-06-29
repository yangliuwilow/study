

### **1、docker 开启远程访问 ,修改配置**

~~~shell
[root@localhost system]# vi /usr/lib/systemd/system/docker.service 
~~~

### **2、配置文件修改ExecStart，设置自己的IP**

~~~shell
ExecStart=/usr/bin/dockerd -H tcp://192.168.7.108:2375 -H unix://var/run/docker.sock
~~~

### **3、刷新配置**

```sheel
[root@localhost system]# systemctl daemon-reload
```

### **4、重新启动docker守护进程。**

~~~shell
[root@localhost system]# systemctl restart docker 
~~~

### 5、访问docker

```java
192.168.7.108:2375/info    //访问成功代码设置成功
```

#### 6、创建Dockerfile

在src/main/docker目录下创建一个名为Dockerfile的文件，配置如下：

```shell
FROM java
VOLUME /tmp
ADD docker-springboot-0.0.1-SNAPSHOT.jar app.jar
RUN bash -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
```

**FROM java：**指Docker Hub上官方提供的java镜像，有了这个基础镜像后，Dockerfile可以通过`FROM`指令直接获取它的状态——也就是在容器中`java`是已经安装的，接下来通过自定义的命令来运行Spring Boot应用。

**VOLUME /tmp：**创建/tmp目录并持久化到Docker数据文件夹，因为Spring Boot使用的内嵌Tomcat容器默认使用`/tmp`作为工作目录。

**ADD docker-springboot-0.0.1-SNAPSHOT.jar app.jar：**将应用jar包复制到`/app.jar`

**ENTRYPOINT：**表示容器运行后默认执行的命令

### 7、pom文件修改

~~~xml
 <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
        <docker.image.prefix>springboot-docker</docker.image.prefix>
</properties>
<build>
    <!--  <defaultGoal>compile</defaultGoal>-->
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <configuration>
                <verbose>true</verbose>
                <fork>true</fork>
                <executable>${JAVA8_HOME}/bin/javac</executable>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <testFailureIgnore>true</testFailureIgnore>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>com.spotify</groupId>
            <artifactId>docker-maven-plugin</artifactId>
            <version>0.4.14</version>
            <configuration>
                <imageName>${docker.image.prefix}/${project.artifactId}</imageName>
                <dockerDirectory>src/main/docker</dockerDirectory>
                <dockerHost>http://192.168.7.108:2375</dockerHost>
                <resources>
                    <resource>
                        <targetPath>/</targetPath>
                        <directory>${project.build.directory}</directory>
                        <include>${project.build.finalName}.jar</include>
                    </resource>
                </resources>
            </configuration>
        </plugin>
    </plugins>
</build>
~~~

`imageName：`指定了镜像的名字

`dockerDirectory：`指定Dockerfile的位置

`dockerHost：`指定Docker远程API地址

`resources：`指那些需要和Dockerfile放在一起，在构建镜像时使用的文件，一般应用jar包需要纳入



#### 6.1、JAVA8_HOME在 maven 中setting.xml 配置

~~~xml
<profiles>
    <profile>  
        <id>custom-compiler</id>  
        <properties>  
            <JAVA8_HOME>C:\Program Files\Java\jdk1.8.0_161</JAVA8_HOME>  
        </properties>  
    </profile>  
</profiles>
<activeProfiles>  
    <activeProfile>custom-compiler</activeProfile>  
</activeProfiles>  
~~~



### 8、构建镜像并发布到远程服务器 

cmd进去项目的目录

```java
mvn clean package docker:build 
```

### 9、查看镜像，运行环境

~~~shell
[root@localhost home]# docker images 
REPOSITORY                                 TAG                 IMAGE ID            CREATED             SIZE
springboot-docker/springboot-docker        latest              4fb457dd9da3        5 minutes ago       672MB

#运行环境 
[root@localhost home]# docker run -d -p 8080:8080  --name springboot-docker  4fb457dd9da3

~~~

### 10、本地访问

~~~java
http://192.168.7.108:8080/hello/say
~~~

文档地址：https://spring.io/guides/gs/spring-boot-docker/