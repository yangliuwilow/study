package com.willow.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/6/27.
 */
public class JVM03 {

    static int count=0;
    static void r(){
        count++;
        r();
    }

    public static void main(String[] args){
       /*try{
           r();
       }catch (Throwable t){
           System.out.println(count);
           t.printStackTrace();

       }*/
       test1();
    }

    public static void test1(){
        System.out.println("#########"+hash("123")); ;
    }
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
