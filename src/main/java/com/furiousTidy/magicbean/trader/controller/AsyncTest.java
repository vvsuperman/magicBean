package com.furiousTidy.magicbean.trader.controller;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncTest {

    @Async
    void test1(){
           for(int i=0;i<10;i++){
               System.out.println("i"+i);
           }
    }

    @Async
    void test2(){
        for(int i=0;i<10;i++){
            System.out.println("j"+i);
        }
    }

}
