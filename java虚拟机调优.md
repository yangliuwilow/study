### 一、java内存结构

![1530088635097](images\1530088635097.png)



 

**虚拟机栈**： 每一个方法对应一个栈帧，方法的参数，局部变量都放在栈帧

​                   每一个线程都拥有一个栈空间，在线程中调用一个方法的时候就会启动一个栈帧，没一个栈帧中保存这个方法用到的局部变量。

**堆**：所有new出来的都放在  heap里面

**方法区（永久区）：**   perm开头

​       class文件信息，静态变量，字符串常量，常量池 这类放在 永久区；

**在JVM中共享数据空间划分如下图所示** 

![908514-20160728195713028-1922699910](images\908514-20160728195713028-1922699910.jpg)

 



1.JVM中共享数据空间可以分成三个大区，新生代（Young Generation）、老年代（Old Generation）、永久代（Permanent Generation），其中JVM堆分为新生代和老年代

2.新生代可以划分为三个区，Eden区（存放新生对象），两个幸存区（From Survivor和To Survivor）（存放每次垃圾回收后存活的对象）

3.永久代管理class文件、静态对象、属性等 

Eden：From Survivor:To Survivor=8:1:1

新生代:老年代=1:3 或者1:2

**回收的过程:**

新建对象在Eden区，进行一次GC后有，有引用的对象复制到Survivor区，没引用的对象被回收。

经过多次GC后，对象没有被回收进入  **老年代**

创建的大对象直接放**老年代**

**引用类型：强、软、弱、虚、**

**如何确定垃圾：**

-   引用计数：

-   正向可达（当前确定垃圾的算法）：

    从roots对象计算可以到达的对象



**垃圾收集算法**：

​     **标记清除**（mark-sweep）:缺点 不连续，内存碎片化

​     **拷贝 coping** （Survivor区使用这个算法） ：把内存分为2部分，A区满后拷贝到B区，B区满后存活对象拷贝A区，缺点：服务器内存浪费； 

​     **标记压缩(**mark-compact老年代使用这个算法)：按顺序拷贝存放，可用和不可用的内存

### 二、JVM参数

**- **       标准参数，所有jVM都应该支持

-X      非标准，每个jvm实现都不同

-XX   不稳定参数，下一个版本可能会取消

bjmashibing(马士兵微信)

### 三、java对象的分配

- 栈上分配

  - 线程私有小对象
  - 无逃逸
  - 支持标量替换
  - 无需调整

- 线程本地分配TLAB(Thread Locla Allacation Buffer )
   -  占用eden去， 每个线程默认分配1%(每个线程都有一个线程本地空间，)
   -  多线程的时候不用竞争eden就可以申请空间，提高效率
   -  小对象
   -  无需调整
-  老年代
   -  大对象

  

### JVM参数说明

```java
-XX:-UseTLABTLAB Thread  Local Allocation Buffer  //关闭线程本地缓存区   （线程本地缓存区在eden区）
  
-XX:-DoEscapeAnalysis 关闭逃逸分析 

-XX:-EliminateAllocations  不做标量 分配

-XX:+PrintGCDetails   打印GC详情

-XX:+PrintGC


idea JVM参数设置  (+EliminateAllocations)  加号代表开启
- server  -XX:-DoEscapeAnalysis   -XX:-EliminateAllocations -XX:+UseTLAB -XX:+PrintGC 

-XX:HeapDumpPath     //堆内存信息输出地址，
-Xms分配堆最小内存，  //默认为物理内存的1/64；
-Xmx分配最大内存，    //默认为物理内存的1/4。

-server -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=c:\jvm.dump -XX:+PrintGCDetails -Xms10m -Xmx10m

```



~~~java
Heap
 PSYoungGen      total 213760K, used 46802K [0x2f2c0000, 0x3dc00000, 0x3dc00000)  //新生代
  eden space 212736K, 21% used [0x2f2c0000,0x32070a70,0x3c280000)
  from space 1024K, 1% used [0x3c280000,0x3c284000,0x3c380000)
  to   space 1024K, 0% used [0x3db00000,0x3db00000,0x3dc00000)
 ParOldGen       total 43776K, used 574K [0x12000000, 0x14ac0000, 0x2f2c0000)  //老年代
  object space 43776K, 1% used [0x12000000,0x1208f970,0x14ac0000)
 Metaspace       used 1895K, capacity 2244K, committed 2368K, reserved 4480K //永久区
~~~



