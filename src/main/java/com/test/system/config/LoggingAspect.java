package com.test.system.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Aspect for logging execution of service and controller methods.
 * Provides automatic logging of method entry, exit, and exceptions.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    /**
     * Pointcut for all repository methods
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void repositoryMethods() {}

    /**
     * Log controller method execution with request details
     */
    @Around("controllerMethods()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        // Get HTTP request details
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String requestInfo = "";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestInfo = String.format("%s %s", request.getMethod(), request.getRequestURI());
        }
        
        log.info("→ Controller: {}.{} | Request: {}", 
            className.substring(className.lastIndexOf('.') + 1), 
            methodName, 
            requestInfo);
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("← Controller: {}.{} | Completed in {}ms", 
                className.substring(className.lastIndexOf('.') + 1), 
                methodName, 
                executionTime);
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("✗ Controller: {}.{} | Failed after {}ms | Error: {}", 
                className.substring(className.lastIndexOf('.') + 1), 
                methodName, 
                executionTime,
                e.getMessage());
            throw e;
        }
    }

    /**
     * Log service method execution
     */
    @Around("serviceMethods()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.debug("→ Service: {}.{} | Args: {}", 
            className.substring(className.lastIndexOf('.') + 1), 
            methodName,
            Arrays.toString(args));
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.debug("← Service: {}.{} | Completed in {}ms", 
                className.substring(className.lastIndexOf('.') + 1), 
                methodName, 
                executionTime);
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("✗ Service: {}.{} | Failed after {}ms | Error: {}", 
                className.substring(className.lastIndexOf('.') + 1), 
                methodName, 
                executionTime,
                e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Log repository exceptions only (to avoid too much noise)
     */
    @AfterThrowing(pointcut = "repositoryMethods()", throwing = "exception")
    public void logRepositoryException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("✗ Repository: {}.{} | Database error: {}", 
            className.substring(className.lastIndexOf('.') + 1), 
            methodName,
            exception.getMessage(), exception);
    }

    /**
     * Log all exceptions from any layer
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods() || repositoryMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("Exception in {}.{}: {}", 
            className, 
            methodName, 
            exception.getMessage(), 
            exception);
    }
}

