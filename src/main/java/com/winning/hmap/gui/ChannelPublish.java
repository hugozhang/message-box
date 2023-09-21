package com.winning.hmap.gui;

/**
 * Created with IntelliJ IDEA.
 *
 * @auther: hugo.zxh
 * @date: 2022/06/08 18:06
 * @description:
 */
import redis.clients.jedis.Jedis;

import java.net.InetAddress;

public class ChannelPublish {

    public static void main(String[] args) {

        Jedis jedis = null;

        try {
            /* Creating Jedis object for connecting with redis server */
//            jedis = new Jedis("192.168.90.57",6379);
            jedis = new Jedis("192.168.18.90",6379);

            InetAddress localHost = InetAddress.getLocalHost();
            /* Publishing message to channel C1 */
//            jedis.publish("saas:winning@001", "http://www.qq.com");
            jedis.publish("91b72fb120e03b49f70a298cc982fbda", "http://www.qq.com");

        } catch(Exception ex) {

            System.out.println("Exception : " + ex.getMessage());
        } finally {

//            if(jedis != null) {
//                jedis.close();
//            }
        }
    }
}
