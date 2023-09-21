package com.winning.hmap.util;

import java.util.Base64;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: hugo.zxh
 * @date: 2023/09/12 16:27
 */
public class Base64Utils {


    public static String encodeToString(String original) {
        return Base64.getEncoder().encodeToString(original.getBytes());
    }

    public static void main(String[] args) {
        System.out.println(Base64Utils.encodeToString("123456"));
    }
}
