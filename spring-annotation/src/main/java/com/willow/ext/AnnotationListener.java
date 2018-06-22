package com.willow.ext;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Created by Administrator on 2018/6/22.
 */
@Component
public class AnnotationListener {

    @EventListener(classes={ApplicationEvent.class})
    public void listen(ApplicationEvent event){
        System.out.println("AnnotationListener...监听到的事件："+event);
    }
}
