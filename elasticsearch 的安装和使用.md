# 一、elasticsearch  基本使用

###1、docker安装 elasticsearch 

```shell

#docker  search elasticsearch    //搜索
#docker pull registry.docker-cn.com/library/elastiscearch   //下载 
或者
[root@localhost ~]# docker pull elasticsearch  下载

# docker  images

docker run -e ES_JAVA_OPTS="-Xms256m -Xmx256m" -d -p 9200:9200 -p 9300:9300  --name ES01  镜像Id

# 默认占用2个G内存，限制内存大小，初始大小Xms256m ，最大Xmx256m
```

### 2.测试安装

打开postman 访问：192.168.16.200:9200/   返回JSON：就是安装成功了

~~~json
{
    "name": "uFD8Cwu",
    "cluster_name": "elasticsearch",
    "cluster_uuid": "OPjbrYU7SbeyALyl3TT5Rg",
    "version": {
        "number": "5.6.9",
        "build_hash": "877a590",
        "build_date": "2018-04-12T16:25:14.838Z",
        "build_snapshot": false,
        "lucene_version": "6.6.1"
    },
    "tagline": "You Know, for Search"
}
~~~

###3.文档地址：

https://www.elastic.co/guide/cn/elasticsearch/guide/current/_indexing_employee_documents.html

### 4.发送PUT 数据

输入地址：192.168.16.200:9200/megacorp/employee/1 选择PUT请求数据：

```
{
    "first_name" : "John",
    "last_name" :  "Smith",
    "age" :        25,
    "about" :      "I love to go rock climbing",
    "interests": [ "sports", "music" ]
}
```

注意，路径 /megacorp/employee/1 包含了三部分的信息：

 ~~~json

 ~~~

注意，路径 `/megacorp/employee/1` 包含了三部分的信息：

- `megacorp`

  索引名称

- `employee`

  类型名称

- `1`

  特定雇员的ID

选择Body->raw->JSON格式提交数据-->点击send发送，--返回结果：看Body

![1528611149252](images\es1.jpg)

 继续添加2个数据：

```json
PUT /megacorp/employee/2
{
    "first_name" :  "Jane",
    "last_name" :   "Smith",
    "age" :         32,
    "about" :       "I like to collect rock albums",
    "interests":  [ "music" ]
}

PUT /megacorp/employee/3
{
    "first_name" :  "Douglas",
    "last_name" :   "Fir",
    "age" :         35,
    "about":        "I like to build cabinets",
    "interests":  [ "forestry" ]
}
```



二、Spring Boot 使用elasticsearch

####  添加jest支持 
POM修改添加，自动启动在JestAutoConfiguration 中

```xml
    <!--- elasticsearch 工具jest  version 根据安装的es版本来选择 -->
    <dependency>
        <groupId>io.searchbox</groupId>
        <artifactId>jest</artifactId>
        <version>5.3.3</version>
    </dependency>
```
2.yml配置es服务地址

```
spring:     
    elasticsearch:
      jest:
         uris: 192.168.16.200:9200
```

