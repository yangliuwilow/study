package com.willow.lambda;

import org.junit.Test;

import java.util.function.Consumer;
import java.util.prefs.PreferenceChangeListener;

public class TestLambda {
    public static void main(String[] args) {
        Object obj = new Object();
        System.gc();
        System.out.println();
        obj = new Object();
        obj = new Object();
        System.gc();
        System.out.println();
    }

    @Test
    public void test1(){
        new Thread(()-> System.out.println("abc")).start();
    }
}
