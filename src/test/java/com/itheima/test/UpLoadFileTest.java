package com.itheima.test;

import org.junit.jupiter.api.Test;

public class UpLoadFileTest {
    @Test
    public void test01(){
        String fileName = "error.jpg";
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
    }
}
