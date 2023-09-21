package com.winning.hmap.gui;

/**
 * Created with IntelliJ IDEA.
 *
 * @auther: hugo.zxh
 * @date: 2022/06/08 17:54
 * @description:
 */
import javafx.application.Platform;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class ChannelSubscribe {


    private String redisHost = "192.168.90.57";

    private int port = 6379;

    private Jedis jedis;

    private JedisPubSub jedisPubSub;

    private MessageBoxApplication application;

    public ChannelSubscribe(MessageBoxApplication application) {

        this.application = application;

        try {
            jedis = new Jedis(redisHost,port);

            jedisPubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    System.out.println("Channel " + channel + " has sent a message : " + message );
                    if(channel.equals("C1")) {
                        /* Unsubscribe from channel C1 after first message is received. */
                        unsubscribe(channel);
                    }
                }

                @Override
                public void onSubscribe(String channel, int subscribedChannels) {

//                    SwingUtilities.invokeLater(application::showStage);

                    Platform.runLater(() -> {
                        //javaFX operations should go here
                        System.out.println(1);
                    });

                    System.out.println("Client is Subscribed to channel : "+ channel);
                    System.out.println("Client is Subscribed to "+ subscribedChannels + " no. of channels");
                }

                @Override
                public void onUnsubscribe(String channel, int subscribedChannels) {
                    System.out.println("Client is Unsubscribed from channel : "+ channel);
                    System.out.println("Client is Subscribed to "+ subscribedChannels + " no. of channels");
                }

            };
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
//            if(jedis != null) {
//                jedis.close();
//            }
        }
    }

    public void subscribe(String... channels) {
        jedis.subscribe(jedisPubSub, channels);
    }


    public static void main(String[] args) {

    }
}
