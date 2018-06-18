package com.willow.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

import java.util.Arrays;

/**
 * Created by Administrator on 2018/6/16.
 */
@Aspect
public class LogAspects {

    //抽取公共的切入点表达式
    //1、本类引用
    //2、其他的切面引用
    @Pointcut("execution(* * com.willow.aop.Calculate.*(..))")
    public void pointCut(){};
    //前置通知
    @Before("execution(* com.willow.aop.Calculate.*(..) )")
    public void logStart(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();
        System.out.println("****"+joinPoint.getSignature().getName()+"运行。。。@Before:参数列表是：{"+ Arrays.asList(args)+"}");

    }
    //后置通知
    @After("execution(* com.willow.aop.Calculate.*(..) )")
    public void logEnd(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();
        System.out.println("****"+joinPoint.getSignature().getName()+"运行。。。@After:参数列表是：{"+ Arrays.asList(args)+"}");

    }
    //返回通知
    @AfterReturning(value="execution(* com.willow.aop.Calculate.*(..) )",returning = "result")
    public void logReturn(JoinPoint joinPoint,Object result){
        Object[] args = joinPoint.getArgs();
        System.out.println("方法返回"+joinPoint.getSignature().getName()+"运行。。。@result:返回：{"+ result+"}");
    }
    //异常通知
    @AfterThrowing(value="execution(* com.willow.aop.Calculate.*(..) )",throwing="exception")
    public void logThrowing(JoinPoint joinPoint,Exception exception){
        Object[] args = joinPoint.getArgs();
        System.out.println("方法返回异常"+joinPoint.getSignature().getName()+"运行。。。@AfterThrowing:参数列表是：{"+ Arrays.asList(args)+"}");
    }

}
