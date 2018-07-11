package com.willow.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/6/27.
 */
public class JVM02 {

    public static void main(String[] args){
        List<Object> list=new ArrayList<>();
        for (int i=0;i<100000;i++)
            list.add(new byte[1024*1024]);
    }
}
