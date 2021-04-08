package com.example.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class AspectJTest {
    public AspectJTest() {
    }

    @Pointcut("execution(public * com.example.demo.aspectj.AspectJDemo.*(..))")
    public void pointcutMethod() {
    }

    @Around("pointcutMethod()")
    public Object injectApi(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature)proceedingJoinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();
        Object[] args = proceedingJoinPoint.getArgs();
        System.out.println("shizhikang - injectApi - before:" + methodName);
        Object result = proceedingJoinPoint.proceed();
        System.out.println("shizhikang - injectApi - end:" + methodName);
        return result;
    }
}