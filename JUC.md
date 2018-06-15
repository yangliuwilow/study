#### ConcurrentHashMap

- ConcurrentHashMap 同步容器类是 Java5 增加的一个线程安全的哈希表;介于 HashMap 与 Hashtable 之间;
  内部采用"锁分段"机制替代Hashtable的独占锁,进而提高性能;
- 默认16个段，每个段都是独立的锁，（1.8以后采用CAS，）

### 二、CountDownLatch  介绍

 ~~~java
// 测试类: 计算多线程的执行时间
public class TestCountDownLatch{
    public static void main(String[] args){
        final CountDownLatch latch = new CountDownLatch(10);  //设置线程数为10，每个线程执行完成后数量减1，当数量为0时候主线程执行
        LatchDemo ld = new LatchDemo(latch);
        long start = System.currentTimeMillis();
        // 创建10个线程
        for(int i=0; i<10; i++){
            new Thread(ld).start();
        }

        try{
            latch.await();
        }catch(InterruptedException e){

        }

        long end = System.currentTimeMillis();

        System.out.println("耗费时间为:"+(end - start));

    }
}

class LatchDemo implements Runnable{
    private CountDownLatch latch;

    // 有参构造器
    public LatchDemo(CountDownLatch latch){
        this.latch = latch;
    }

    public void run(){

        synchronized(this){
            try{
                // 打印50000以内的偶数
                for(int i=0; i<50000; i++){
                    if(i % 2 == 0){
                        System.out.println(i);
                    }
                }
            }finally{
                // 每个线程执行完毕后数量递减
                latch.countDown();
            }
        }
    }
}
 ~~~

### 5. 创建执行线程的方式三Callable

- 相较于实现 Runnable 接口的方式,实现 Callable 接口类中的方法可以有返回值,并且可以抛出异常;

  

  ~~~java
  // 测试类
  public class TestCallable{
      public static void main(String[] args){
  
          ThreadDemo td = new ThreadDemo();
  
          // 执行 Callable 方式,需要 FutureTask 实现类的支持
          // FutureTask 实现类用于接收运算结果, FutureTask 是 Future 接口的实现类
          FutureTask<Integer> result = new FutureTask<>(td);
  
          new Thread(result).start();
  
          // 接收线程运算后的结果
          try{
              // 只有当 Thread 线程执行完成后,才会打印结果;
              // 因此, FutureTask 也可用于闭锁
              Integer sum = result.get();
              System.out.println(sum);
          }catch(InterruptedException | ExecutionException e){
              e.printStackTrace();
          }
      }
  }
  
  class ThreadDemo implements Callable<Integer>{
  
      // 需要实现的方法
      public Integer call() throws Exception{
          // 计算 0~100 的和
          int sum = 0;
  
          for(int i=0; i<=100; i++){
              sum += i;
          }
  
          return sum;
      }
  }
  ~~~

  ### 6. 同步锁(Lock)

     synchronized  :隐式锁

  ​     1：同步方法块

  ​     2：同步方法

  jdk 1.5以后

  ​      3：同步锁Lock

  需要通过lock()方法上锁，通过unlock()方法进行释放锁

  ~~~java
  // 测试类: 以卖票为例
  // 使用 lock 之前
  public class TestLock{
      public static void main(String[] args){
          Ticket ticket = new Ticket();
  
          new Thread(ticket,"1号窗口").start();
          new Thread(ticket,"2号窗口").start();
          new Thread(ticket,"3号窗口").start();
      }
  }
  
  class Ticket implements Runnable{
  
      private int tick = 100;
  
      public void run(){
          while(true){
              if(tick > 0){
                  try{
                      Thread.sleep(200);
                  }catch(InterruptedException e){
  
                  }
  
                  System.out.println(Thread.currentThread().getName()+"完成售票,余票为: "+ --tick);
              }
          }
      }
  }
  
  // 使用 Lock
  class Ticket implements Runnable{
  
      private int tick = 100;
  
      private Lock lock = new ReentrantLock();
  
      public void run(){
          while(true){
              // 上锁
              lock.lock();
  
              try{
                  if(tick > 0){
                      try{
                          Thread.sleep(200);
                      }catch(InterruptedException e){
  
                      }
                      System.out.println(Thread.currentThread().getName()+"完成售票,余票为: "+ --tick);
                  }
              }finally{
                  // 释放锁
                  lock.unlock();
              }
          }
      }
  }
  
  ~~~

  https://blog.csdn.net/zxm1306192988/article/details/59701101

​                           