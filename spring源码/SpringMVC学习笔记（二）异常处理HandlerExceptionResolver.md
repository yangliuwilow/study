# SpringMVC学习笔记（二）异常处理HandlerExceptionResolver

doDispatch:967, DispatcherServlet 断点此处，查看 size = 1 size = 3

查看DispatcherServlet 中 默认handlerExceptionResolvers  的

```
ExceptionHandlerExceptionResolver
ResponseStatusExceptionResolver
DefaultHandlerExceptionResolver
```

### 一、ExceptionHandlerExceptionResolver功能

@****





SpringMVC学习笔记（二） 视图解析器





```
HandlerMapping //请求到处理器之前的映射
HandlerAdapter// hanler适配器
```