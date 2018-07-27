package com.willow.jvm;

/**
 * Created by Administrator on 2018/6/27.
 */
public class JVM01 {
    class User {
        int id;
        String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

    }
    void alloc(int i){
        new User(i,"name"+i);
    }
    public static void main(String[] args){
        System.out.println("total："+Runtime.getRuntime().totalMemory());   //线程总内存
        System.out.println("free："+Runtime.getRuntime().freeMemory());            //使用内存
        JVM01 jvm=new JVM01();
        long s1=System.currentTimeMillis();
        for (int i=0;i<10000000;i++)  jvm.alloc(i);
        long s2=System.currentTimeMillis();
        System.out.println(s2-s1);
        System.out.println("total："+Runtime.getRuntime().totalMemory());
        System.out.println("free："+Runtime.getRuntime().freeMemory());
    }
}
