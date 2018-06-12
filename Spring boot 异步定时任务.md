## Spring boot 异步/定时任务/邮件：

### 一、异步功能

1：启动类添加注解：

```java
@EnableAsync   //开启异步注解功能
```

2：方法上添加：

```java
//告诉Spring这是一个异步方法
@Async
public void hello(){
    try {
        Thread.sleep(3000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    System.out.println("处理数据中...");
}
```

###二、定时任务

1:启动类开启定时任务：

~~~java
@EnableScheduling   
~~~

2：方法上面

~~~Java
/**
     * second(秒), minute（分）, hour（时）, day of month（日）, month（月）, day of week（周几）.
     * 0 * * * * MON-FRI
     *  【0 0/5 14,18 * * ?】 每天14点整，和18点整，每隔5分钟执行一次
     *  【0 15 10 ? * 1-6】 每个月的周一至周六10:15分执行一次
     *  【0 0 2 ? * 6L】每个月的最后一个周六凌晨2点执行一次
     *  【0 0 2 LW * ?】每个月的最后一个工作日凌晨2点执行一次
     *  【0 0 2-4 ? * 1#1】每个月的第一个周一凌晨2点到4点期间，每个整点都执行一次；
     */
   // @Scheduled(cron = "0 * * * * MON-SAT")
   //@Scheduled(cron = "0,1,2,3,4 * * * * MON-SAT")
   // @Scheduled(cron = "0-4 * * * * MON-SAT")
    @Scheduled(cron = "0/4 * * * * MON-SAT")  //每4秒执行一次
    public void hello(){
        System.out.println("hello ... ");
}
~~~

cron表达式：

| 字段 | 允许值                  | 允许的特殊字符    |
| ---- | ----------------------- | ----------------- |
| 秒   | 0-59                    | , -   * /         |
| 分   | 0-59                    | , -   * /         |
| 小时 | 0-23                    | , -   * /         |
| 日期 | 1-31                    | , -   * ? / L W C |
| 月份 | 1-12                    | , -   * /         |
| 星期 | 0-7或SUN-SAT   0,7是SUN | , -   * ? / L C # |

特殊字符 

| 特殊字符 | 代表含义                      |
| -------- | ----------------------------- |
| ,        | 枚举                          |
| -        | 区间                          |
| *        | 任意                          |
| /        | 步长                          |
| ?        | 日/星期冲突匹配               |
| L        | 最后                          |
| W        | 工作日                        |
| C        | 和calendar联系后计算过的值    |
| #        | 第几个 星期，4#2，第2个星期四 |

### 三、邮件任务

####     3.1.添加POM依赖

```xml
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-mail</artifactId>  <!--- 邮件-->
</dependency>
```

自动启动类：MailSenderAutoConfiguration  ，属性配置类：MailProperties

####     3.2  配置文件

~~~properties
spring.mail.username=#####@qq.com
spring.mail.password=123456   
spring.mail.host=smtp.qq.com
spring.mail.properties.mail.smtp.ssl.enable=true
~~~

####     3.3代码部分

~~~java
@Autowired
	JavaMailSenderImpl mailSender;

	@Test
	public void contextLoads() {
		SimpleMailMessage message = new SimpleMailMessage();
		//邮件设置
		message.setSubject("通知-今晚开会");
		message.setText("今晚7:30开会");

		message.setTo("17512080612@163.com");
		message.setFrom("534096094@qq.com");

		mailSender.send(message);
	}

	@Test
	public void test02() throws  Exception{
		//1、创建一个复杂的消息邮件
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

		//邮件设置
		helper.setSubject("通知-今晚开会");
		helper.setText("<b style='color:red'>今天 7:30 开会</b>",true);

		helper.setTo("17512080612@163.com");
		helper.setFrom("534096094@qq.com");

		//上传文件
		helper.addAttachment("1.jpg",new File("C:\\Users\\lfy\\Pictures\\Saved Pictures\\1.jpg"));
		helper.addAttachment("2.jpg",new File("C:\\Users\\lfy\\Pictures\\Saved Pictures\\2.jpg"));

		mailSender.send(mimeMessage);

	}

~~~



