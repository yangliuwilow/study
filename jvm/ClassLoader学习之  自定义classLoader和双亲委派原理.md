# ClassLoader 学习笔记



### 1、ClassLoader原理介绍

​        ClassLoader使用的是**双亲委托模型**来搜索类的，每个ClassLoader实例都有一个父类加载器的引用（不是继承的关系，是一个包含的关系），虚拟机内置的类加载器（Bootstrap ClassLoader）本身没有父类加载器，但可以用作其它ClassLoader实例的的父类加载器。当一个ClassLoader实例需要加载某个类时，它会试图亲自搜索某个类之前，先把这个任务委托给它的父类加载器，这个过程是由上至下依次检查的，

### 2、为什么要使用双亲委托这种模型呢？ 

​        因为这样可以避免重复加载，当父亲已经加载了该类的时候，就没有必要子ClassLoader再加载一次。考虑到安全因素，我们试想一下，如果不使用这种委托模式，那我们就可以随时使用自定义的String来动态替代java核心api中定义的类型，这样会存在非常大的安全隐患，而双亲委托的方式，就可以避免这种情况，因为String已经在启动时就被引导类加载器（Bootstrcp ClassLoader）加载，所以用户自定义的ClassLoader永远也无法加载一个自己写的String，除非你改变JDK中ClassLoader搜索类的默认算法。 

### 3、ClassLoader的体系架构： 



![classLoader](..\images\loader.png)

 

#### 类加载器间的关系

我们进一步了解类加载器间的关系(并非指继承关系)，主要可以分为以下4点

- 启动类加载器，由C++实现，没有父类。
- 拓展类加载器(ExtClassLoader)，由Java语言实现，父类加载器为null
- 系统类加载器(AppClassLoader)，由Java语言实现，父类加载器为ExtClassLoader
- ​自定义类加载器，父类加载器肯定为AppClassLoader。         

顶层的类加载器是ClassLoader类，它是一个抽象类，其后所有的类加载器都继承自ClassLoader（不包括启动类加载器），这里我们主要介绍ClassLoader中几个比较重要的方法。 

~~~java
protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
      synchronized (getClassLoadingLock(name)) {
          // 先从缓存查找该class对象，找到就不用重新加载
          Class<?> c = findLoadedClass(name);
          if (c == null) {
              long t0 = System.nanoTime();
              try {
                  if (parent != null) {
                      //如果找不到，则委托给父类加载器去加载
                      c = parent.loadClass(name, false);
                  } else {
                  //如果没有父类，则委托给启动加载器去加载
                      c = findBootstrapClassOrNull(name);
                  }
              } catch (ClassNotFoundException e) {
                  // ClassNotFoundException thrown if class not found
                  // from the non-null parent class loader
              }

              if (c == null) {
                  // If still not found, then invoke findClass in order
                  // 如果都没有找到，则通过自定义实现的findClass去查找并加载
                  c = findClass(name);

                  // this is the defining class loader; record the stats
                  sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                  sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                  sun.misc.PerfCounter.getFindClasses().increment();
              }
          }
          if (resolve) {//是否需要在加载时进行解析
              resolveClass(c);
          }
          return c;
      }
  }
~~~

​         Lancher初始化时首先会创建ExtClassLoader类加载器，然后再创建AppClassLoader并把ExtClassLoader传递给它作为父类加载器，这里还把AppClassLoader默认设置为线程上下文类加载器 

~~~java
public Launcher() {
    // 首先创建拓展类加载器
    ClassLoader extcl;
    try {
        extcl = ExtClassLoader.getExtClassLoader();
    } catch (IOException e) {
        throw new InternalError(
            "Could not create extension class loader");
    }

    // Now create the class loader to use to launch the application
    try {
        //再创建AppClassLoader并把extcl作为父加载器传递给AppClassLoader
        loader = AppClassLoader.getAppClassLoader(extcl);
    } catch (IOException e) {
        throw new InternalError(
            "Could not create application class loader");
    }

    //设置线程上下文类加载器，稍后分析
    Thread.currentThread().setContextClassLoader(loader);
    //省略其他没必要的代码......
}
}
~~~



### 4、创建自定义类加载器，继承ClassLoader

~~~java
package com.willow.classloader;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 自定义类加载器
 */
public class FileSystemClassLoader extends ClassLoader {

    private String rootDir;  //指定这个类加载器加载的目录  d:/study/

    public FileSystemClassLoader(String rootDir) {
        this.rootDir = rootDir;
    }

    //编写获取类的字节码并创建class对象的逻辑
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);//查找这个类是否加载了
        if (c != null) {
            return c;
        }
        ClassLoader parent = this.getParent(); //获取到父类加载器
        try {
            c = parent.loadClass(name); //委派给父类加载
        }catch (ClassNotFoundException e){

        }

        if (c != null) {
            return c;
        } else {
            try {
                byte[] classData=getClassData(name);
                if(classData==null){
                    throw new ClassNotFoundException();
                }else{
                    //方法接受一组字节，然后将其具体化为一个Class类型实例，它一般从磁盘上加载一个文件，然后将文件的字节传递给JVM，通过JVM（native 方法）对于Class的定义，将其具体化，实例化为一个Class类型实例。
                    c=defineClass(name,classData,0,classData.length);
                    return c;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        return c;
    }

    /**
     * 编写读取字节流的方法
     *
     * @param className
     * @return
     * @throws IOException
     */
    private byte[] getClassData(String className) throws IOException {  //com.willow.entity.user   转换为： d:/study/  com/willow/entity/user
        String path = rootDir + "/" + className.replace('.', '/') + ".class";

        InputStream inputStream = null;
        ByteOutputStream outputStream = new ByteOutputStream();
        try {
            inputStream = new FileInputStream(path); //读取需要加载的类
            byte[] buffer = new byte[1024];
            int temp = 0;
            while ((temp = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, temp);
            }
            return outputStream.toByteArray();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }

        return null;
    }
}

~~~

**测试自定义类加载器**

~~~java
public class MyClassLoderTest {

    public static void main(String[] args) {

        //打印ClassLoader类的层次结构
        ClassLoader classLoader = MyClassLoderTest.class.getClassLoader();    //获得加载ClassLoaderTest.class这个类的类加载器
        while(classLoader != null) {
            System.out.println(classLoader);
            classLoader = classLoader.getParent();    //获得父类加载器的引用
        }
        System.out.println(classLoader);


        //测试自定义类加载器  ，在d:/study 创建HelloWorld.java ，然后编译为HelloWorld.class 文件
        FileSystemClassLoader loader=new FileSystemClassLoader("d:/study");
        FileSystemClassLoader loader2=new FileSystemClassLoader("d:/study");
        try {
            Class<?> c = loader.loadClass("HelloWorld");
            Class<?> c1 = loader.loadClass("HelloWorld");
            Class<?> c3 = loader2.loadClass("HelloWorld");


            Class<?> cString = loader2.loadClass("java.lang.String");
            Class<?> cMyClassLoderTest = loader2.loadClass("com.willow.classloader.MyClassLoderTest");
            System.out.println(c.hashCode()+"##:classLoader"+c.getClassLoader());    //自定义加载器加载
            System.out.println(c1.hashCode());
            System.out.println(c3.hashCode());  //同一个类，不同的加载器加载，JVM 认为也是不相同的类
            System.out.println(cString.hashCode()+"##:classLoader"+cString.getClassLoader());  //引导类加载器
            System.out.println(cMyClassLoderTest.hashCode()+"##:classLoader"+cMyClassLoderTest.getClassLoader());  //系统默认加载器
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
~~~








参考文章：https://blog.csdn.net/javazejian/article/details/73413292#