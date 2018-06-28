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
       try{
           r();
       }catch (Throwable t){
           System.out.println(count);
           t.printStackTrace();

       }

    }
}
