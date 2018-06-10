# Spring boot 集合elasticsearche使用

###1.使用jest

添加jest支持

POM修改添加，自动启动在JestAutoConfiguration 中,配置类：JestProperties

```xml
<!--- elasticsearch 工具jest  version 根据安装的es版本来选择 -->
<dependency>
    <groupId>io.searchbox</groupId>
    <artifactId>jest</artifactId>
    <version>5.3.3</version>
</dependency>
<dependency>
    <groupId>com.sun.jna</groupId>
    <artifactId>jna</artifactId>
    <version>3.0.9</version>
</dependency
```
2.yml配置es服务地址

```yaml
spring:     
    elasticsearch:
      jest:
         uris: http://192.168.16.200:9200
```

3.简单使用，添加和搜索

~~~java
     @Autowired
	JestClient jestClient;

	@Test
	public void contextLoads() {
		//1、给Es中索引（保存）一个文档；
		Article article = new Article();
		article.setId(1);
		article.setTitle("好消息");
		article.setAuthor("zhangsan");
		article.setContent("Hello World");

		//构建一个索引功能
		Index index = new Index.Builder(article).index("atguigu").type("news").build();

		try {
			//执行
			jestClient.execute(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//测试搜索
	@Test
	public void search(){

		//查询表达式
		String json ="{\n" +
				"    \"query\" : {\n" +
				"        \"match\" : {\n" +
				"            \"content\" : \"hello\"\n" +
				"        }\n" +
				"    }\n" +
				"}";

		//更多操作：https://github.com/searchbox-io/Jest/tree/master/jest
		//构建搜索功能
		Search search = new Search.Builder(json).addIndex("atguigu").addType("news").build();

		//执行
		try {
			SearchResult result = jestClient.execute(search);
			System.out.println(result.getJsonString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

~~~

###2.使用jest（方法二）

注意：Springboot 添加的elasticsearch  版本为2.4.6 ，所有安装的elasticsearch   应该为2.4.6 

1.POM文件添加需要的依赖

~~~xml
 <!-- elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
~~~



  2.yml配置文件 

```properties
spring.data.elasticsearch.cluster-name=elasticsearch
spring.data.elasticsearch.cluster-nodes=192.168.16.200:9300
```

cluster-name 通过get请求：http://192.168.16.200:9200/  访问返回：

~~~json
{
    "name": "Jacqueline Falsworth",
    "cluster_name": "elasticsearch", #对应配置文件的cluster-name
    "cluster_uuid": "m3tOxtVoRrWLnDGG4xexuw",
    "version": {
        "number": "2.4.6",  #版本号，和导入的jar版本需要对应
        "build_hash": "5376dca9f70f3abef96a77f4bb22720ace8240fd",
        "build_timestamp": "2017-07-18T12:17:44Z",
        "build_snapshot": false,
        "lucene_version": "5.5.4"
    },
    "tagline": "You Know, for Search"
}
~~~



3.使用：

3. 1book实体类配置：

~~~java
package com.atguigu.elastic.bean;

import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "atguigu",type = "book")
public class Book {
    private Integer id;
    private String bookName;
    private String author;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", bookName='" + bookName + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}

~~~

3.2 Repository 创建

~~~java
package com.atguigu.elastic.repository;

import com.atguigu.elastic.bean.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;


public interface BookRepository extends ElasticsearchRepository<Book,Integer> {

    //参照
    // https://docs.spring.io/spring-data/elasticsearch/docs/3.0.6.RELEASE/reference/html/
   public List<Book> findByBookNameLike(String bookName);

}

~~~

测试功能

~~~java
@Autowired
	BookRepository bookRepository;
	//保存
    @Test
	public void test01(){
 		Book book = new Book();
 		book.setId(1);
 		book.setBookName("西游记");
 		book.setAuthor("吴承恩");
 		bookRepository.index(book);
    }
	@Test
	public void test01(){
 		//查询
		for (Book book : bookRepository.findByBookNameLike("游")) {
			System.out.println(book);
		}
		 

	}
~~~


​      说明：
```java
/**
 * SpringBoot默认支持两种技术来和ES交互；
 * 1、Jest（默认不生效）
 * 	需要导入jest的工具包（io.searchbox.client.JestClient）
 * 2、SpringData ElasticSearch【ES版本有可能不合适】
 * 		jar版本适配说明：https://github.com/spring-projects/spring-data-elasticsearch
 *		如果版本不适配：2.4.6(Spring boot 中导入的es为2.4.6,不支持自己安装es版本，重新安装es版本)
 *			1）、升级SpringBoot版本
 *			2）、安装对应版本的ES
 *
 * 		1）、Client 节点信息clusterNodes；clusterName(需要配置的)
 * 		2）、ElasticsearchTemplate 操作es
 *		3）、编写一个 ElasticsearchRepository 的子接口来操作ES；
 *	两种用法：https://github.com/spring-projects/spring-data-elasticsearch
 *	1）、编写一个 ElasticsearchRepository
 */
```


​      