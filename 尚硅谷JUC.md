在 Java 5.0 提供了 java.util.concurrent （简称JUC ）包,在此包中增加了在并发编程中很常用的实用工具类，用于定义类似于线程的自定义子系统，包括线程池、异步 IO 和轻量级任务框架。提供可调的、灵活的线程池。还提供了设计用于多线程上下文中的 Collection 实现等。

### 一、volatile关键字、内存可见性

**内存可见性**

内存可见性（Memory Visibility）是指当某个线程正在使用对象状态而另一个线程在同时修改该状态，需要确保当一个线程修改了对象状态后，其他线程能够看到发生的状态变化。

可见性错误是指当读操作与写操作在不同的线程中执行时，我们无法确保执行读操作的线程能适时地看到其他线程写入的值，有时甚至是根本不可能的事情。

我们可以通过同步来保证对象被安全地发布。除此之外我们也可以使用一种更加轻量级的 volatile 变量。

**volatile 关键字**

Java 提供了一种稍弱的同步机制，即 volatile 变量，用来确保将变量的更新操作通知到其他线程，**可以保证内存中的数据可见**。可以将 volatile 看做一个轻量级的锁，但是又与锁有些不同：

- 对于多线程，不是一种互斥关系
- 不能保证变量状态的“原子性操作”

```
public class TestVolatile {
    public static void main(String[] args){
        ThreadDemo td=new ThreadDemo();
        new Thread(td).start();
        while(true){
            if(td.isFlag()){
                System.out.println("-----------");
                break;
            }
        }
    }

}
class ThreadDemo implements Runnable{
    private volatile boolean flag=false;
    public void run() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        flag=true;
        System.out.println("flag="+isFlag());
    }
    public boolean isFlag(){
        return flag;
    }
    public void setFlag(boolean flag){
        this.flag=flag;
    }
}12345678910111213141516171819202122232425262728293031
```

运行结果：

```
-----------
flag=true12
```

如果不加 volatile 关键字，只会输出它，且程序死循环 ：

```
flag=true1
```

volatile 关键字保证了flag变量对所有线程内存课件，所以当flag变量 值变化后，主线程 while 循环中检测到，打印后 程序执行完成，退出；如果 flag 不加 volatile 关键字，主线程将一直while 死循环 ，不退出。

### 二、原子变量 、CAS

原子变量：jdk1.5 后 java.util.concurrent.atomic 类的小工具包，支持在单个变量上解除锁的线程安全编程，包下提供了常用的原子变量： 
\- AtomicBoolean 、AtomicInteger 、AtomicLong 、 AtomicReference 
\- AtomicIntegerArray 、AtomicLongArray 
\- AtomicMarkableReference 
\- AtomicReferenceArray 
\- AtomicStampedReference

1.类中的变量都是volatile类型：保证**内存可见性** 
2.使用CAS算法：保证**数据的原子性**

CAS (Compare-And-Swap) 是一种硬件对并发的支持，针对多处理器操作而设计的处理器中的一种特殊指令，用于管理对共享数据的并发访问。 
CAS 是一种无锁的非阻塞算法的实现。 
CAS包含三个操作数： 
内存值 V 
预估值 A 
更新值 B 
当且仅当V==A时，B的值才更新给A，否则将不做任何操作。

```
public class TestAtomicDemo {

    public static void main(String[] args) {
        AtomicDemo ad = new AtomicDemo();

        for (int i = 0; i < 10; i++) {
            new Thread(ad).start();
        }
    }

}

class AtomicDemo implements Runnable{

//  private volatile int serialNumber = 0;

    private AtomicInteger serialNumber = new AtomicInteger(0);

    @Override
    public void run() {

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }

        System.out.println(getSerialNumber());
    }

    public int getSerialNumber(){
        return serialNumber.getAndIncrement();//i++ 实际是int temp=i;i=i+1;i=temp; 需要原子性操作
    }
}123456789101112131415161718192021222324252627282930313233
```

CAS 算法实际是**由硬件机制完成**的，我们使用synchronized方法模拟CAS 算法，用10个线程代表对内存中数据的10次修改请求。只有上个线程修改完，这个线程从内存中获取的内存值当成期望值，才等于内存值，才能对内存值进行修改。

```
public class TestCompareAndSwap {
    public static void main(String[] args) {
        final CompareAndSwap cas=new CompareAndSwap();

        for(int i=0;i<10;i++){
            new Thread(new Runnable(){
                @Override
                public void run() { 
                    int expectedValue=cas.get();
                    boolean b=cas.compareAndSwap(expectedValue, (int)(Math.random()*101));
                    System.out.println(b);
                }
            }).start();
        }
    }
}

class CompareAndSwap{
    private int value;//内存值

    //获取内存值
    public synchronized int get(){
        return value;
    }

    //比较
    public synchronized boolean compareAndSwap(int expectedValue,int newValue){
        int oldValue=value;//线程读取内存值，与预估值比较
        if(oldValue==expectedValue){
            this.value=newValue;
            return true;
        }
        return false;
    }
}1234567891011121314151617181920212223242526272829303132333435
```

### 三、ConcurrentHashMap、锁分段

HashMap 线程不安全 
Hashtable 内部采用独占锁，线程安全，但效率低 
ConcurrentHashMap同步容器类是java5 新增的一个线程安全的哈希表，效率介于HashMap和Hashtable之间。内部采用“锁分段”机制。

java.util.concurrent 包还提供了设计用于多线程上下文中的Collection实现：

当期望许多线程访问一个给定 collection 时， 
ConcurrentHashMap 通常优于同步的 HashMap， 
ConcurrentSkipListMap 通常优于同步的 TreeMap 
ConcurrentSkipListSet通常优于同步的 TreeSet.

当期望的读数和遍历远远大于列表的更新数时， 
CopyOnWriteArrayList 优于同步的 ArrayList。因为每次添加时都会进行复制，开销非常的大，并发迭代操作多时 ，选择。

### 四、CountDownLatch 闭锁

CountDownLatch 一个**同步辅助类**，在完成一组正在其他线程中执行的操作之前，它允许一个或多个线程一直等待。

闭锁可以延迟线程的进度直到其到达终止状态，闭锁可以用来确保某些活动直到其他活动都完成才继续执行： 
 确保某个计算在其需要的所有资源都被初始化之后才继续执行; 
 确保某个服务在其依赖的所有其他服务都已经启动之后才启动; 
 等待直到某个操作所有参与者都准备就绪再继续执行。

```
/*
 * CountDownLatch:闭锁，在完成某些运算时，只有其他所有线程的运算全部完成，当前运算才继续执行
 */
public class TestCountDownLatch {
    public static void main(String[] args) {
        final CountDownLatch latch=new CountDownLatch(50);
        LatchDemo ld=new LatchDemo(latch);

        long start=System.currentTimeMillis();

        for(int i=0;i<50;i++){
            new Thread(ld).start();
        }

        try {
            latch.await(); //直到50个人子线程都执行完，latch的值减到0时，才往下执行
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long end=System.currentTimeMillis();
        System.out.println("耗费时间为："+(end-start));
    }
}

class LatchDemo implements Runnable{
    private CountDownLatch latch;

    public LatchDemo(CountDownLatch latch){
        this.latch=latch;
    }
    @Override
    public void run() {
        try{
            for(int i=0;i<50000;i++){
                if(i%2==0){
                    System.out.println(i);
                }
            }
        }finally{
            latch.countDown();//latch的值减一
        }
    }
}1234567891011121314151617181920212223242526272829303132333435363738394041424344
```

### 五、实现Callable接口

Java 5.0 在 java.util.concurrent 提供了一个新的**创建执行线程**的方式：Callable 接口

实现Callable 接口，相较于实现 Runnable接口的方式，方法可以有返回值，并且可以抛出异常。

Callable 需要依赖FutureTask ，用于接收返回值，FutureTask 也可以用作闭锁。

```
/*
 * 一、创建执行线程的方式三：实现Callable接口。相较于实现Runnable接口的方式，方法可以有返回值，并且可以抛出异常。
 * 二、执行Callable方式，需要FutureTask实现类的支持，用于接收运算结果。FutureTask是Future接口的实现类
 */
public class TestCallable {
    public static void main(String[] args) {
        ThreadDemo2 td=new ThreadDemo2();

        //1.执行Callable方式，需要FutureTask实现类的支持，用于接收运行结果。
        FutureTask<Integer> result=new FutureTask<>(td);
        new Thread(result).start();

        //2.接收线程运算后的结果
        try {
            Integer sum = result.get();//FutureTask 可用于 闭锁  当子线程执行完毕，才会执行此后语句
            System.out.println(sum);
            System.out.println("----------------------");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } 
    }
}

class ThreadDemo2 implements Callable<Integer>{

    @Override
    public Integer call() throws Exception {
        int sum=0;
        for(int i=0;i<=100000;i++){
            sum+=i;
        }
        return sum;
    }

}1234567891011121314151617181920212223242526272829303132333435
```

### 六、Lock 同步锁

在 Java 5.0 之前，协调共享对象的访问时可以使用的机制只有 synchronized 和 volatile 。Java 5.0 后增加了一些新的机制，但并不是一种替代内置锁的方法，而是当内置锁不适用时，作为一种可选择的高级功能。

ReentrantLock 实现了 Lock 接口，并提供了与synchronized 相同的互斥性和内存可见性。但相较于synchronized 提供了更高的处理锁的灵活性。

```
/*
 * 一、用于解决多线程安全问题的方式：
 * synchronized:隐式锁
 * 1、同步代码块
 * 2、同步方法
 * jdk 1.5后
 * 3、同步锁 Lock  
 * 注意：是一个显式锁，通过lock()方式上锁，必须通过unlock()方法释放锁
 */
public class TestLock {
    public static void main(String[] args) {
        Ticket ticket=new Ticket();
        new Thread(ticket,"1号窗口").start();
        new Thread(ticket,"2号窗口").start();
        new Thread(ticket,"3号窗口").start();
    }
}

class Ticket implements Runnable{
    private int tick=100;
    private Lock lock=new ReentrantLock();
    @Override
    public void run() {
        while(true){
            lock.lock();
            try{
                if(tick>0){
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName()+"完成售票为："+--tick);
                }
                else{
                    break;
                }
            }finally{
                lock.unlock();//释放锁一定要放在finally里，保证一定执行
            }
        }
    }

}1234567891011121314151617181920212223242526272829303132333435363738394041424344
```

```
/*
 * 生产者和消费者案例,优化，防止出现虚假唤醒，线程无法停止
 */
public class TestProductorAndConsumer {
    public static void main(String[] args) {
        Clerk clerk=new Clerk();

        Productor pro=new Productor(clerk);
        Consumer cus=new Consumer(clerk);

        new Thread(pro,"生产者 A").start();
        new Thread(cus,"消费者 B").start();
        new Thread(pro,"生产者 C").start();
        new Thread(cus,"消费者 D").start();
    }
}

//店员 假如只有一个商品位置
class Clerk{
    private int product=0;

    //进货
    public synchronized void get(){
        while(product>=1){//为了避免虚假唤醒问题，应该总是使用在循环中
            System.out.println("产品已满！");
            try{
                this.wait();
            }catch(InterruptedException e){
            }
        }
        System.out.println(Thread.currentThread().getName()+" : "+ ++product);
        this.notifyAll();
    }

    //卖货
    public synchronized void sale(){
        while(product<=0){
            System.out.println("缺货！");
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(Thread.currentThread().getName()+" : "+ --product);
        this.notifyAll();
    }
}

//生产者
class Productor implements Runnable{
    private Clerk clerk;
    public Productor(Clerk clerk){
        this.clerk=clerk;
    }
    @Override
    public void run() {
        for(int i=0;i<20;i++){
            try{
                Thread.sleep(200);
            }catch(InterruptedException e){
            }
            clerk.get();
        }
    }
}

//消费者
class Consumer implements Runnable{
    private Clerk clerk;
    public Consumer(Clerk clerk){
        this.clerk=clerk;
    }
    @Override
    public void run() {
        for(int i=0;i<20;i++){
            clerk.sale();
        }
    }
}1234567891011121314151617181920212223242526272829303132333435363738394041424344454647484950515253545556575859606162636465666768697071727374757677787980
```

### 七、Condition 控制线程通信

Condition 接口描述了可能会与锁有关联的条件变量。这些变量在用法上与使用 Object.wait 访问的隐式监视器类似，但提供了更强大的功能。需要特别指出的是，单个 Lock 可能与多个 Condition 对象关联。为了避免兼容性问题，Condition 方法的名称与对应的 Object 版本中的不同。

在 Condition 对象中，与 wait、notify 和 notifyAll 方法对应的分别是await、signal 和 signalAll。

Condition 实例实质上被绑定到一个锁上。要为特定 Lock 实例获得Condition 实例，请使用其 newCondition() 方法。

```
public class TestProductorAndConsumerForLock {
    public static void main(String[] args) {
        Clerk clerk=new Clerk();

        Productor pro=new Productor(clerk);
        Consumer cus=new Consumer(clerk);

        new Thread(pro,"生产者 A").start();
        new Thread(cus,"消费者 B").start();
        new Thread(pro,"生产者 C").start();
        new Thread(cus,"消费者 D").start();
    }
}

//店员 假如只有一个商品位置
class Clerk{
    private int product=0;
    private Lock lock=new ReentrantLock();
    private Condition condition=lock.newCondition();

    //进货
    public void get(){
        lock.lock();

        try{
            while(product>=1){//为了避免虚假唤醒问题，应该总是使用在循环中
                System.out.println("产品已满！");
                try{
                    condition.await();//this.wait();
                }catch(InterruptedException e){
                }
            }
            System.out.println(Thread.currentThread().getName()+" : "+ ++product);
            condition.signalAll();//this.notifyAll();
        }finally{
            lock.unlock();
        }
    }

    //卖货
    public void sale(){
        lock.lock();

        try{
            while(product<=0){
                System.out.println("缺货！");
                try {
                    condition.await();//this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(Thread.currentThread().getName()+" : "+ --product);
            condition.signalAll();//this.notifyAll();
        }finally{
            lock.unlock();
        }
    }
}

//生产者
class Productor implements Runnable{
    private Clerk clerk;
    public Productor(Clerk clerk){
        this.clerk=clerk;
    }
    @Override
    public void run() {
        for(int i=0;i<20;i++){
            try{
                Thread.sleep(200);
            }catch(InterruptedException e){
            }
            clerk.get();
        }
    }
}

//消费者
class Consumer implements Runnable{
    private Clerk clerk;
    public Consumer(Clerk clerk){
        this.clerk=clerk;
    }
    @Override
    public void run() {
        for(int i=0;i<20;i++){
            clerk.sale();
        }
    }
}12345678910111213141516171819202122232425262728293031323334353637383940414243444546474849505152535455565758596061626364656667686970717273747576777879808182838485868788899091
```

### 八、线程按序交替

编写一个程序，开启 3 个线程，这三个线程的 ID 分别为A、B、C，每个线程将自己的 ID 在屏幕上打印 10 遍，要求输出的结果必须按顺序显示。如：ABCABCABC…… 依次递归

```
public class TestABCAlternate {
    public static void main(String[] args) {
        AlternateDemo ad=new AlternateDemo();
        new Thread(new Runnable(){
            @Override
            public void run() {
                for(int i=1;i<=20;i++){
                    ad.loopA(i);
                }
            }

        },"A").start();

        new Thread(new Runnable(){
            @Override
            public void run() {
                for(int i=1;i<=20;i++){
                    ad.loopB(i);
                }
            }

        },"B").start();

        new Thread(new Runnable(){
            @Override
            public void run() {
                for(int i=1;i<=20;i++){
                    ad.loopC(i);
                    System.out.println("-----------------------------------");
                }
            }

        },"C").start();
    }
}

class AlternateDemo{
    private int number=1;//当前正在执行线程的标记
    private Lock lock=new ReentrantLock();
    private Condition condition1=lock.newCondition();
    private Condition condition2=lock.newCondition();
    private Condition condition3=lock.newCondition();

    /*
     * @param totalLoop:循环第几轮
     */
    public void loopA(int totalLoop){
        lock.lock();
        try{
            //1.判断
            if(number!=1){
                condition1.await();
            }

            //2.打印
            System.out.println(Thread.currentThread().getName()+"\t"+totalLoop);

            //3.唤醒
            number=2;
            condition2.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            lock.unlock();
        }
    }

    public void loopB(int totalLoop){
        lock.lock();
        try{
            //1.判断
            if(number!=2){
                condition2.await();
            }

            //2.打印
            System.out.println(Thread.currentThread().getName()+"\t"+totalLoop);

            //3.唤醒
            number=3;
            condition3.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            lock.unlock();
        }
    }

    public void loopC(int totalLoop){
        lock.lock();
        try{
            //1.判断
            if(number!=3){
                condition3.await();
            }

            //2.打印
            System.out.println(Thread.currentThread().getName()+"\t"+totalLoop);

            //3.唤醒
            number=1;
            condition1.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            lock.unlock();
        }
    }
}123456789101112131415161718192021222324252627282930313233343536373839404142434445464748495051525354555657585960616263646566676869707172737475767778798081828384858687888990919293949596979899100101102103104105106107108109
```

### 九、ReadWriteLock 读写锁

ReadWriteLock 维护了一对相关的锁，一个用于只读操作，另一个用于写入操作。只要没有 writer，读取锁可以由多个 reader 线程同时保持。写入锁是独占的。

ReadWriteLock 读取操作通常不会改变共享资源，但执行写入操作时，必须独占方式来获取锁。对于读取操作占多数的数据结构。 ReadWriteLock 能提供比独占锁更高的并发性。而对于只读的数据结构，其中包含的不变性可以完全不需要考虑加锁操作。

```
/*
 * 1.ReadWriteLock:读写锁
 * 写写|读写   需要"互斥"
 * 读读       不需要"互斥"
 */
public class TestReadWriteLock {
    public static void main(String[] args) {
        ReadWriteLockDemo rw=new ReadWriteLockDemo();
        new Thread(new Runnable() {
            @Override
            public void run() {
                rw.set((int)(Math.random()*101));
            }
        },"Write").start();

        for(int i=0;i<100;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    rw.get();
                }
            }).start();
        }
    }
}

class ReadWriteLockDemo{
    private int number=0;
    private ReadWriteLock lock=new ReentrantReadWriteLock();

    //读
    public void get(){
        lock.readLock().lock();//上锁
        try{
            System.out.println(Thread.currentThread().getName()+" : "+number);
        }finally{
            lock.readLock().unlock();//释放锁
        }
    }

    //写
    public void set(int number){
        lock.writeLock().lock();
        try{
            System.out.println(Thread.currentThread().getName());
            this.number=number;
        }finally{
            lock.writeLock().unlock();
        }
    }
}123456789101112131415161718192021222324252627282930313233343536373839404142434445464748495051
```

### 十、线程八锁

- 一个对象里面如果有多个synchronized方法，某一个时刻内，只要一个线程去调用其中的一个synchronized方法了，其它的线程都只能等待，换句话说，某一个时刻内，只能有唯一一个线程去访问这些synchronized方法。锁的是当前对象this，被锁定后，其它的线程都不能进入到当前对象的其它的synchronized方法。
- 加个普通方法后发现和同步锁无关
- 换成两个对象后，不是同一把锁了，情况立刻变化。
- 都换成静态同步方法后，情况又变化
- 所有的非静态同步方法用的都是同一把锁——实例对象本身，也就是说如果一个实例对象的非静态同步方法获取锁后，该实例对象的其他非静态同步方法必须等待获取锁的方法释放锁后才能获取锁，可是别的实例对象的非静态同步方法因为跟该实例对象的非静态同步方法用的是不同的锁，所以毋须等待该实例对象已获取锁的非静态同步方法释放锁就可以获取他们自己的锁。
- 所有的静态同步方法用的也是同一把锁——类对象本身，这两把锁是两个不同的对象，所以静态同步方法与非静态同步方法之间是不会有竞态条件的。但是一旦一个静态同步方法获取锁后，其他的静态同步方法都必须等待该方法释放锁后才能获取锁，而不管是同一个实例对象的静态同步方法之间，还是不同的实例对象的静态同步方法之间，只是它们同一个类的实例对象！

```
/*
 * 实验：观察打印的"one" or "two" ?
 * 
 * 1.两个普通同步方法，两个线程，标准打印，打印？  //one two  因为同步锁是this(调用对象本身),被锁定后，其它的线程都不能进入到当前对象的其它的synchronized方法
 * 2.新增Thread.sleep()给 getOne(),打印？ //等3秒后  one two
 * 3.新增普通方法(非同步) getThree(),打印？//three 等3秒 one two 因为同步锁不影响普通方法的执行
 * 4.两个普通同步方法，两个Number对象，打印？//two 等3秒 one  因为用的不是同一把锁，互不影响
 * 5.修改 getOne() 为静态同步方法，使用一个Number对象打印?  //two 等3秒 one  因为静态同步方法用的锁是类对象本身，Number.class; 和对象用的是不同的锁
 * 6.修改两个方法均为静态同步方法，一个Number对象？//等3秒 one two 用的锁都是Number类对象本身
 * 7.一个静态同步方法，一个非静态同步方法，两个Number对象？//two 等3秒one 
 * 8.两个静态同步方法，两个Number对象？//等3秒后  one two 用的锁都是Number类对象本身
 * 
 * 线程八锁的关键：
 * ①非静态方法的锁默认为  this,  静态方法的锁为 对应的 Class 实例
 * ②某一个时刻内，只能有一个线程持有同一把锁，无论几个方法。
 */

public class TestThread8Monitor {
    public static void main(String[] args) {
        Number number = new Number();
        Number number2 = new Number();
        new Thread(new Runnable() {
            public void run() {
                number.getOne();
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
//              number.getTwo();
                number2.getTwo();
            }
        }).start();
//      new Thread(new Runnable() {
//          public void run() {
//              number.getThree();
//          }
//      }).start();

    }

}

class Number {
    public static synchronized void getOne() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        System.out.println("one");
    }

    public static synchronized void getTwo() {
        System.out.println("two");
    }

//  public void getThree(){
//      System.out.println("three");
//  }
}1234567891011121314151617181920212223242526272829303132333435363738394041424344454647484950515253545556575859
```

### 十一、线程池

获取线程第四种方法。 
线程池可以解决两个不同问题：由于减少了每个任务调用的开销，它们通常可以在执行大量异步任务时提供增强的性能，并且还可以提供绑定和管理资源（包括执行任务集时使用的线程）的方法。每个 ThreadPoolExecutor 还维护着一些基本的统计数据，如完成的任务数。

为了便于跨大量上下文使用，此类提供了很多可调整的参数和扩展钩子 (hook)。但是，强烈建议程序员使用较为方便的 Executors 工厂方法 ：

- Executors.newCachedThreadPool()（无界线程池，可以进行自动线程回收）
- Executors.newFixedThreadPool(int)（固定大小线程池）
- Executors.newSingleThreadExecutor()（单个后台线程）

它们均为大多数使用场景预定义了设置。

```
/*
 * 一、线程池：提供了一个线程队列，队列中保存着所有等待状态的线程。避免了创建与销毁额外开销，提高了响应的速度。
 * 二、线程池的体系结构：
 *    java.util.concurrent.Executor:负责线程的使用与调度的根接口
 *        |--**ExecutorService 子接口：线程池的主要接口
 *             |--ThreadPoolExecutor 线程池的实现类
 *             |--ScheduledExecutorService 子接口：负责线程的调度
 *                  |--ScheduledThreadPoolExecutor:继承ThreadPoolExecutor,实现ScheduledExecutorService接口
 * 三、工具类：Executors
 * 方法有：
 * ExecutorService newFixedThreadPool(): 创建固定大小的线程池
 * ExecutorService newCachedThreadPool():缓存线程池，线程池的数量不固定，可以根据需要自动的更改数量。
 * ExecutorService newSingleThreadExecutor():创建单个线程池。线程池中只有一个线程
 * 
 * ScheduledExecutorService newScheduledThreadPool():创建固定大小的线程，可以延迟或定时的执行任务。
 * 
 */
public class TestThreadPool {
    public static void main(String[] args) {
        //1.创建线程池
        ExecutorService pool=Executors.newFixedThreadPool(5);

        List<Future<Integer>> list=new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Future<Integer> future=pool.submit(new Callable<Integer>(){
                @Override
                public Integer call() throws Exception {
                    int sum=0;
                    for(int i=0;i<=100;i++){
                        sum+=i;
                    }
                    return sum;
                }

            });
            list.add(future);
        }
        pool.shutdown();
        for(Future<Integer> future:list){
            try {
                System.out.println(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
123456789101112131415161718192021222324252627282930313233343536373839404142434445464748
```

### 十二、线程调度

```
public class TestScheduledThreadPool {
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService pool=Executors.newScheduledThreadPool(5);
        for (int i = 0; i < 5; i++) {
            Future<Integer> result=pool.schedule(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int num=new Random().nextInt(100);//生成随机数
                    System.out.println(Thread.currentThread().getName()+" : "+num);
                    return num;
                }

            }, 2, TimeUnit.SECONDS);//每次延迟两秒后运行
            System.out.println(result.get());
        }
    }
}1234567891011121314151617
```

### 十三、ForkJoinPool 分支/ 合并框架 工作窃取

Fork/Join 框架：就是在必要的情况下，将一个大任务，进行拆分(fork)成若干个小任务（拆到不可再拆时），再将一个个的小任务运算的结果进行 join 汇总。

![这里写图片描述](https://img-blog.csdn.net/20170305170116170?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvenhtMTMwNjE5Mjk4OA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

**Fork/Join 框架与线程池的区别：** 
采用 “工作窃取”模式（work-stealing）：相对于一般的线程池实现，fork/join框架的优势体现在对其中包含的任务的处理方式上.在一般的线程池中，如果一个线程正在执行的任务由于某些原因无法继续运行，那么该线程会处于等待状态。而在fork/join框架实现中，如果某个子问题由于等待另外一个子问题 的完成而无法继续运行。那么处理该子问题的线程会主动寻找其他尚未运行的子问题来执行.这种方式减少了线程的等待时间，提高了性能。

```java
public class TestForkJoinPool {
    public static void main(String[] args) {
        Instant start=Instant.now();
        ForkJoinPool pool=new ForkJoinPool();
        ForkJoinTask<Long> task=new ForkJoinSumCalculate(0L, 50000000000L);
        Long sum=pool.invoke(task);
        System.out.println(sum);
        Instant end=Instant.now();
        System.out.println("耗费时间为："+Duration.between(start, end).toMillis());//耗费时间为：21020
    }

    //一般的方法
    @Test
    public void test1(){
        Instant start=Instant.now();
        long sum=0L;
        for(long i=0L;i<=50000000000L;i++){
            sum+=i;
        }
        System.out.println(sum);
        Instant end=Instant.now();
        System.out.println("耗费时间为："+Duration.between(start, end).toMillis());//耗费时间为：27040
    }

    //java8 新特性
    @Test
    public void test2(){
        Instant start=Instant.now();
        Long sum=LongStream.rangeClosed(0L,50000000000L).parallel().reduce(0L, Long::sum);
        System.out.println(sum);
        Instant end=Instant.now();
        System.out.println("耗费时间为："+Duration.between(start, end).toMillis());//耗费时间为：14281
    }

}

class ForkJoinSumCalculate extends RecursiveTask<Long>{
    private static final long serialVersionUID=-54565646543212315L;

    private long start;
    private long end;

    private static final long THURSHOLD=10000L;//临界值，小于这个值就不拆了，直接运算

    public ForkJoinSumCalculate(long start,long end){
        this.start=start;
        this.end=end;
    }
    @Override
    protected Long compute() {
        long length=end-start;
        if(length<=THURSHOLD){
            long sum=0L;
            for(long i=start;i<=end;i++){
                sum+=i;
            }
            return sum;
        }else{
            //进行拆分，同时压入线程队列
            long middle=(start+end)/2;
            ForkJoinSumCalculate left=new ForkJoinSumCalculate(start, middle);
            left.fork();
            ForkJoinSumCalculate right=new ForkJoinSumCalculate(middle+1, end);
            right.fork();
            return left.join()+right.join();
        }
    }

}
```