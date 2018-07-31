# Redis 学习笔记（三）   键(key)常用操作

### 1、Redis 键(key)常用操作

参考文档：http://www.runoob.com/redis/redis-tutorial.html

参考文档：http://redisdoc.com/

#### 1.1   keys *    查看当前库所有的key 

~~~shell
127.0.0.1:6379> keys *
1) "name"
~~~

#### 1.2  exists key的名字，判断某个key是否存在

~~~shell
127.0.0.1:6379[2]> EXISTS  name
(integer) 1		
~~~

#### 1.3 move key db   --->当前库就没有了，被移除了

~~~shell
127.0.0.1:6379> move name 2
(integer) 1
127.0.0.1:6379> get name 
(nil)
127.0.0.1:6379> select 2
OK
127.0.0.1:6379[2]> get name  
"willow"

~~~

#### 1.4 expire key为给定的key设置过期时间

~~~shell
127.0.0.1:6379[2]> EXPIRE name 10   # 设置10s
(integer) 1
127.0.0.1:6379[2]> ttl name         # 查看还有多少秒过期   -1表示永不过期，-2表示已过期
(integer) 5
127.0.0.1:6379[2]> ttl name
(integer) 2
127.0.0.1:6379[2]> ttl name 
(integer) -2
127.0.0.1:6379[2]> get name 
(nil)

~~~

#### 1.5 、type key 查看key数据类型

~~~shell
127.0.0.1:6379[2]> type name
string
~~~



### Redis 键相关的基本命令： 	 

| 序号 | 命令及描述                                                   |
| ---- | ------------------------------------------------------------ |
| 1    | [DEL key](http://www.runoob.com/redis/keys-del.html) 该命令用于在 key 存在时删除 key。 |
| 2    | [DUMP key](http://www.runoob.com/redis/keys-dump.html)  序列化给定 key ，并返回被序列化的值。 |
| 3    | [EXISTS key](http://www.runoob.com/redis/keys-exists.html)  检查给定 key 是否存在。 |
| 4    | [EXPIRE key](http://www.runoob.com/redis/keys-expire.html) seconds 为给定 key 设置过期时间。 |
| 5    | [EXPIREAT key timestamp](http://www.runoob.com/redis/keys-expireat.html)  EXPIREAT 的作用和 EXPIRE 类似，都用于为 key 设置过期时间。 不同在于 EXPIREAT 命令接受的时间参数是 UNIX 时间戳(unix timestamp)。 |
| 6    | [PEXPIRE key milliseconds](http://www.runoob.com/redis/keys-pexpire.html)  设置 key 的过期时间以毫秒计。 |
| 7    | [PEXPIREAT key milliseconds-timestamp](http://www.runoob.com/redis/keys-pexpireat.html)  设置 key 过期时间的时间戳(unix timestamp) 以毫秒计 |
| 8    | [KEYS pattern](http://www.runoob.com/redis/keys-keys.html)  查找所有符合给定模式( pattern)的 key 。 |
| 9    | [MOVE key db](http://www.runoob.com/redis/keys-move.html)  将当前数据库的 key 移动到给定的数据库 db 当中。 |
| 10   | [PERSIST key](http://www.runoob.com/redis/keys-persist.html)  移除 key 的过期时间，key 将持久保持。 |
| 11   | [PTTL key](http://www.runoob.com/redis/keys-pttl.html)  以毫秒为单位返回 key 的剩余的过期时间。 |
| 12   | [TTL key](http://www.runoob.com/redis/keys-ttl.html)  以秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)。 |
| 13   | [RANDOMKEY](http://www.runoob.com/redis/keys-randomkey.html)  从当前数据库中随机返回一个 key 。 |
| 14   | [RENAME key newkey](http://www.runoob.com/redis/keys-rename.html)  修改 key 的名称 |
| 15   | [RENAMENX key newkey](http://www.runoob.com/redis/keys-renamenx.html)  仅当 newkey 不存在时，将 key 改名为 newkey 。 |
| 16   | [TYPE key](http://www.runoob.com/redis/keys-type.html)  返回 key 所储存的值的类型。 |