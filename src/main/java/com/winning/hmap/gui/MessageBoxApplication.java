package com.winning.hmap.gui;


import com.winning.hmap.service.RpcService;
import com.winning.hmap.service.Retrofit2Holder;
import com.winning.hmap.util.Base64Utils;
import com.winning.hmap.util.MD5Utils;
import com.winning.hmap.vo.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.JedisShardInfo;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.MenuItem;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;


/**
 * 消息盒子  主程序
 *
 * @auther: hugo.zxh
 * @date: 2022/06/07 11:06
 * @description:
 */
public class MessageBoxApplication extends Application {

    private static Logger logger = LoggerFactory.getLogger(MessageBoxApplication.class);

    /**
     * 应用配置相关
     */

    private String redisHost;

    private int redisPort;

    private String redisPwd;

    private Jedis jedis;

    private JedisPubSub jedisPubSub;

    private ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
    
    /**
     * ui 相关
     */

    private static final AudioClip MSG_AUDIO = new AudioClip(MessageBoxApplication.class.getResource("/msg2.mp3").toString());

    private static final URL TRAY_ICON_URL = MessageBoxApplication.class.getResource("/GameCenter16x16.png");

    private static Image TRAY_ICON_IMG = null;

    static {
        try {
            TRAY_ICON_IMG = ImageIO.read(TRAY_ICON_URL);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    private Timer notificationTimer = new Timer();

    private Stage loginStage;

    private Preferences pref;

    private TrayIcon trayIcon;

    /**
     * 绑定相关
     */
    private String username;

    private String password;

    private boolean isLogin;

    private boolean isSelected;

    /**
     * 系统变量
     */
    private String sysTenantCode;

    private String sysServerHttp;

    private RpcService rpcService;

    private void initSystemSettings() {
        Properties properties = new Properties();
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(new File(System.getProperty("user.dir") + File.separator + "setting.properties"));
            properties.load(inputStream);
            sysTenantCode = properties.getProperty("tenant.code");
            sysServerHttp = properties.getProperty("server.http");
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(),e);
            System.exit(0);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            System.exit(0);
        }

        Retrofit2Holder.init(sysServerHttp);
        rpcService = Retrofit2Holder.getService();
    }

    private void initUserSettings() {
        pref = Preferences.userRoot().node("/setting");
        username = pref.get("tenant","");
        password = pref.get("account","");
    }

    private void initialize() {
        initSystemSettings();
        initUserSettings();
    }


    private void alertBox() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误提示");
        alert.setHeaderText("获取远程配置失败");
//        alert.setContentText("获取远程配置失败");
        alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> System.exit(0));
    }

    private void initRedis() {

        R<ConfigInfo> resp = rpcService.getConfigInfo();
        if (resp == null || resp == null || resp.getCode() !=0) {
            logger.info(resp.getMessage());
            alertBox();
            return;
        }

        ConfigInfo configInfo = resp.getData();
        this.redisHost = configInfo.getRedisHost();
        this.redisPort = configInfo.getRedisPort();
        this.redisPwd = configInfo.getRedisPassword();

        JedisShardInfo jedisShardInfo = new JedisShardInfo(redisHost, redisPort);
        if (redisPwd != null && redisPwd.trim().length() != 0) {
            jedisShardInfo.setPassword(redisPwd);
        }
        jedis = jedisShardInfo.createResource();
        jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                logger.info("接收到来自频道 : " + channel + "，消息内容 : " + message );
                newMsg();
                Platform.runLater(() -> showNotifyUI());
                Platform.runLater(() -> reloadUrl(message));
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                logger.info("成功订阅频道 : "+ channel + "，序号 : " + subscribedChannels);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                logger.info("成功退订频道 : "+ channel + "，序号 : " + subscribedChannels);
            }
        };
        // 发布订阅
        subscribe(this.channel);
    }


    @Override
    public void start(final Stage stage) {
//        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.TRANSPARENT);
//          stage.initStyle(StageStyle.UNIFIED);
//        stage.initStyle(StageStyle.UTILITY);

        logger.info(System.getProperty("user.dir"));

        initialize();

        this.loginStage = stage;
        InputStream asStream = MessageBoxApplication.class.getResourceAsStream("/GameCenter32x32.png");
        this.loginStage.getIcons().add(new javafx.scene.image.Image(asStream));

        Platform.setImplicitExit(false);
        SwingUtilities.invokeLater(this::addAppToTray);

        StackPane layout = new StackPane(createContent());
        Scene scene = new Scene(layout, 600, 400);
        scene.setFill(Color.TRANSPARENT);

        this.loginStage.setTitle("金仕达卫宁");
        this.loginStage.setScene(scene);
        this.loginStage.show();
        this.loginStage.toFront();

        checkVersion();

//        Platform.runLater(() -> initMsgBoxStage());

        Platform.runLater(() -> initNotifyUI());

        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        logger.info("Height: " + screenBounds.getHeight() + " Width: " + screenBounds.getWidth());
    }


    private String version = "v1.0.0";


    private void checkVersionAlertBox(String version) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText("消息盒子当前版本：" + this.version + "，最新版本：" + version + "，如需要升级请联系管理员" );
        alert.initModality(Modality.APPLICATION_MODAL);
//        alert.setContentText("获取远程配置失败");
        alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> System.out.println(123));
    }

    private void checkVersion() {
        R<VerInfo> resp = rpcService.getVerInfo();
        if (resp != null && resp.getCode() == 0) {
            VerInfo verInfo = resp.getData();
            if (verInfo != null && !version.equals(verInfo.getVersion())) {
                //版本不一致
                checkVersionAlertBox(verInfo.getVersion());
            }
        }
    }


    private WebView webView  = new WebView();

    private Stage messageBoxStage = new Stage();

    public void showMsgBoxStage() {
        oldMsg();
        if (messageBoxStage != null) {
            if(!messageBoxStage.isShowing()) { messageBoxStage.show(); }
            messageBoxStage.toFront();
        }
    }

    private void initMsgBoxStage() {

        HBox hbox = new HBox();
        hbox.setSpacing(10);
        hbox.setPadding(new Insets(10));

        //==========头像==============
        VBox vboxTop = new VBox();
        InputStream avatarInputStream = MessageBoxApplication.class.getResourceAsStream("/GameCenter32x32.png");
        ImageView imageView = new ImageView(new javafx.scene.image.Image(avatarInputStream));
        vboxTop.getChildren().addAll(imageView);
        vboxTop.setAlignment(Pos.TOP_CENTER);

        //==========退出==============
        VBox vboxBottom = new VBox();
        Button btnButton = new Button("退出账号");
        vboxBottom.getChildren().addAll(btnButton);
        vboxBottom.setAlignment(Pos.BOTTOM_CENTER);
        btnButton.setOnAction(event -> {
            messageBoxStage.hide();
            showStage();
            loginOut();
        });

        // ========= 左边 ============
        VBox vbox = new VBox();
        VBox.setVgrow(vboxBottom,Priority.ALWAYS);
        vbox.getChildren().addAll(vboxTop,vboxBottom);

        webView  = new WebView();
        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(webView);
        stackPane.setAlignment(Pos.BASELINE_CENTER);
        HBox.setHgrow(stackPane, Priority.ALWAYS);
        createProgressReport(webView.getEngine());

        hbox.getChildren().addAll(vbox, stackPane);


//        WebViews.addHyperlinkListener(webView, event -> {
//            logger.info("正在访问 : " + event.getURL());
//            Desktop d = Desktop.getDesktop();
//            try {
//                d.browse(event.getURL().toURI());
//            } catch (URISyntaxException | IOException e) {
//                logger.error(e.getMessage(),e);
//            }
//            return false;
//        }, HyperlinkEvent.EventType.ACTIVATED);

        Rectangle2D screenBounds = Screen.getPrimary().getBounds();

        Scene scene = new Scene(hbox, screenBounds.getWidth()*2/3, screenBounds.getHeight()*2/3);
        scene.setFill(Color.TRANSPARENT);

        InputStream asStream = MessageBoxApplication.class.getResourceAsStream("/GameCenter32x32.png");

        messageBoxStage.getIcons().add(new javafx.scene.image.Image(asStream));
        messageBoxStage.setScene(scene);
    }

    private Stage notifyStage;

    private void showNotifyUI() {
        if (!notifyStage.isShowing()) {
            MSG_AUDIO.play();
            notifyStage.show();
            notifyStage.toFront();
            notifyStage.setAlwaysOnTop(true);
        }
    }

    private void initNotifyUI() {
        notifyStage = new Stage();
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();

        StackPane stackPane = new StackPane();
        Label textField = new Label();
        textField.setText("您有新消息啦,请点击查看");
        textField.setOnMouseClicked(event -> openBrowse(browseURL));

        stackPane.getChildren().add(textField);
        StackPane.setAlignment(textField, Pos.CENTER);

        Scene scene = new Scene(stackPane,300,100);
        scene.setFill(Color.TRANSPARENT);
        notifyStage.setTitle("卫宁科技 - 消息盒子");

        InputStream asStream = MessageBoxApplication.class.getResourceAsStream("/GameCenter32x32.png");

        notifyStage.initStyle(StageStyle.UTILITY);

        notifyStage.getIcons().add(new javafx.scene.image.Image(asStream));
        notifyStage.setScene(scene);
        notifyStage.setX(screenBounds.getWidth() - scene.getWidth() - 10);
        notifyStage.setY(screenBounds.getHeight() - scene.getHeight() - 100);

        notifyStage.showingProperty().addListener((observable, oldValue, newValue) -> {
            logger.info("old value : {} , new value : {}.",oldValue,newValue);
            if (newValue) {
                logger.info("Stage is showing");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> notifyStage.close());
                    }
                },5000);
            } else {
                logger.info("Stage is not showing");
            }
        });
    }

    private void reloadUrl(String url) {
        webView.getEngine().load(url);
    }

    private Node createContent() {

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setPadding(new Insets(25, 25, 25, 25));
        javafx.scene.image.Image imageX = new javafx.scene.image.Image(getClass().getResource("/backgroud.png").toExternalForm());

        BackgroundSize size = new BackgroundSize(BackgroundSize.AUTO,
                BackgroundSize.AUTO,
                false,
                false,
                true,
                true);

        Background background2 = new Background(new BackgroundImage(imageX,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size));

        grid.setBackground(background2);


        GridPane grid1 = new GridPane();
        Background background3 = new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 1),
                CornerRadii.EMPTY, Insets.EMPTY));
        grid1.setBackground(background3);
        grid1.setAlignment(Pos.CENTER);
        grid1.setHgap(15);
        grid1.setVgap(15);
        grid1.setPadding(new Insets(25, 25, 25, 25));

        Text sceneTitle1 = new Text("              账户密码登录");
        sceneTitle1.setFill(Color.BLACK);
        sceneTitle1.setFont(Font.font("Tahoma", FontWeight.NORMAL, 15));
        grid1.add(sceneTitle1, 0, 0);

        Label accLabel1 = new Label("用户名");
        grid1.add(accLabel1, 0, 1);
        TextField accField1 = new TextField();
        accField1.setText(username);
        grid1.add(accField1, 0, 2);

        Label pwdLabel1 = new Label("密码");
        grid1.add(pwdLabel1, 0, 3);
        TextField pwdField1 = new PasswordField();
        pwdField1.setText(password);
        grid1.add(pwdField1, 0, 4);

        HBox hBox = new HBox();
        hBox.setSpacing(80);
        CheckBox button1 = new CheckBox("记住密码");
        button1.setSelected(isSelected);
        Text text = new Text();
        text.setFill(Color.BLUE);
        text.setText("忘记密码");
        text.setFont(Font.font("PingFangSC-Semibold", FontWeight.BOLD, 13));

        hBox.getChildren().add(button1);
        hBox.getChildren().add(text);

        grid1.add(hBox, 0, 5);

        Button btn = new Button("                     登录                     ");
        btn.setStyle("-fx-font: 15 arial; -fx-base: #1a67fa;");
//        Background background1 = new Background(new BackgroundFill(
//                Color.rgb(27, 103, 251, 1),
//                CornerRadii.EMPTY, Insets.EMPTY));
//        btn.setBackground(background1);
        HBox hbBtn = new HBox(4);

        hbBtn.getChildren().add(btn);
        grid1.add(hbBtn, 0, 6);

        // 结果显示
        actionTarget = new Text();
        grid1.add(actionTarget, 0, 7);

        grid.add(grid1,0, 0, 2, 1);



        btn.setOnAction(e -> this.loginHandler(accField1,pwdField1,button1));
        text.setOnMouseClicked(e -> this.removePwd(accField1,pwdField1));
        return grid;
    }

    private Integer yhId;

    private String ysid;

    private String mryljgdm;

    private String mryljgmc;

    private String channel;

    private Text actionTarget;

    public void loginHandler(TextField accField,TextField pwdField,CheckBox button1) {
        actionTarget.setFill(Color.FIREBRICK);
        String username = accField.getText();
        String password = pwdField.getText();
        if (username == null || username.trim().length() == 0) {
            actionTarget.setText("用户名不能为空");
            return;
        }
        if (password == null || password.trim().length() == 0) {
            actionTarget.setText("密码不能为空");
            return;
        }

        //退订已有的  租户:医疗机构:
        unsubscribe(this.channel);

        //保存订阅信息到本地
        saveSubscribe(username,password,button1);

        //接口服务
        LoginResponse loginResponse = rpcService.login(username, Base64Utils.encodeToString(password), sysTenantCode);
        if (loginResponse == null) {
            actionTarget.setText("登陆异常");
            return;
        } else if(loginResponse.getMessage() != null && loginResponse.getMessage().length() != 0) {
            actionTarget.setText(loginResponse.getMessage());
            return;
        } else {
            User user = loginResponse.getData();
            this.yhId = user.getYhId();
            this.ysid = user.getYsid();
            this.mryljgdm = user.getMryljgdm();
            this.mryljgmc = user.getMryljgmc();
            this.channel = MD5Utils.md5ToHex(mryljgdm + ":" + ysid);
            this.browseURL = sysServerHttp + "/#/home?yhid=" + yhId + "&gid=" + Base64Utils.encodeToString(Retrofit2Holder.setCookieHeader);
            logger.info("channel is {}.",this.channel);
            logger.info("url is {}.",this.browseURL);
        }

        // 结果
        actionTarget.setText("登陆成功");

        updateMessageBoxTitle();

        loginStage.hide();
        // 桌面内容页
        // showMsgBoxStage();
        // 跳到浏览器
        openBrowse(browseURL);
        // 登陆成功初始化监听
        Platform.runLater(() -> initRedis());

    }

    public void removePwd(TextField accField1,TextField pwdField1) {
        accField1.setText("");
        pwdField1.setText("");
        pref.remove("tenant");
        pref.remove("account");
    }


    private Desktop desktop;

    private String browseURL;

    public void openBrowse(String browseURL) {
        oldMsg();
        if (desktop == null) {
            desktop = Desktop.getDesktop();
        }
        try {
            URL urlObj = new URL(browseURL);
            desktop.browse(urlObj.toURI());
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    private void updateMessageBoxTitle() {
        messageBoxStage.setTitle(this.mryljgmc);
    }

    private void saveSubscribe(String tenant,String account,CheckBox checkBox) {
        this.password = account;
        this.username = tenant;
        this.isLogin = true;
        if(checkBox.isSelected()){
            pref.put("tenant",tenant);
            pref.put("account",account);
            isSelected = true;
        }
    }

    private void unsubscribe(String channel) {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe(channel);
        }
    }


    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            Toolkit.getDefaultToolkit();

            if (!SystemTray.isSupported()) {
                logger.info("No system tray support, application exiting.");
                Platform.exit();
            }

            // set up a system tray icon.
            SystemTray tray = SystemTray.getSystemTray();

            Image image = ImageIO.read(TRAY_ICON_URL);

            this.trayIcon = new TrayIcon(image);

//            trayIcon.addActionListener(event -> Platform.runLater(this::showMsgBoxStage));


            trayIcon.addActionListener(event -> Platform.runLater(() -> openBrowse(browseURL)));

            MenuItem settingsItem = new MenuItem("设置页面");
            settingsItem.addActionListener(event -> Platform.runLater(this::showStage));
            MenuItem messageBoxItem = new MenuItem("消息内容");
            messageBoxItem.addActionListener(event -> {
                if (isLogin) {
                    // Platform.runLater(this::showMsgBoxStage);
                    openBrowse(browseURL);
                } else {
                    Platform.runLater(this::showStage);
                }
            });
            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(event -> {
                tray.remove(trayIcon);
                logger.info("点击退出程序");
                exit();
            });

            // setup the popup menu for the application.
            PopupMenu popup = new PopupMenu();
            popup.add(settingsItem);
            popup.addSeparator();
            popup.add(messageBoxItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);
            // add the application tray icon to the system tray.
            tray.add(trayIcon);

            blink();
        } catch (AWTException | IOException e) {
            logger.error("Unable to init system tray",e);
        }
    }


    private void loginOut() {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
        }
        rpcService.loginOut();//远程退出
        isLogin = false;
        actionTarget.setText("已退出,请重新登陆");
    }

    private void exit() {
        loginOut();
        executorService.shutdown();
        notificationTimer.cancel();
        Platform.exit();
        System.exit(0);
    }

    private AtomicBoolean newMsg = new AtomicBoolean(false);

    private void blink() {
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            boolean state = false;
            @Override
            public void run() {
                if (newMsg.get()) {
                    if (state) {
                        trayIcon.setImage(Toolkit.getDefaultToolkit().getImage("GameCenter16x16.png"));
                    } else {
                        trayIcon.setImage(TRAY_ICON_IMG);
                    }
                    state = !state;
                }
            }
        }, 0, 500);
    }

    private void newMsg() {
        newMsg.compareAndSet(false,true);
    }

    private void oldMsg() {
        newMsg.compareAndSet(true,false);
        trayIcon.setImage(TRAY_ICON_IMG);
    }

    public void showStage() {
        if (loginStage != null) {
            if(!loginStage.isShowing()){ loginStage.show(); }
            loginStage.toFront();
        }
    }

    private void createProgressReport(WebEngine engine) {

        LongProperty startTime   = new SimpleLongProperty();
        LongProperty endTime     = new SimpleLongProperty();
        LongProperty elapsedTime = new SimpleLongProperty();

//        ProgressBar loadProgress = new ProgressBar();
//        loadProgress.setVisible(true);
//        loadProgress.progressProperty().bind(engine.getLoadWorker().progressProperty());

//        Label loadTimeLabel = new Label();
//        loadTimeLabel.textProperty().bind(
//                Bindings.when(elapsedTime.greaterThan(0))
//                        .then(Bindings.concat("Loaded page in ", elapsedTime.divide(1_000_000), "ms"))
//                        .otherwise("Loading...")
//        );
        elapsedTime.bind(Bindings.subtract(endTime, startTime));

        engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, state) -> {
            switch (state) {
                case RUNNING:
                    startTime.set(System.nanoTime());
                    break;
                case SUCCEEDED:
//                    loadProgress.setVisible(false);
                    endTime.set(System.nanoTime());
                    logger.info("WebView加载完成,elapsed time : {} ms.",elapsedTime.divide(1_000_000));
                    break;
            }
        });

//        HBox progressReport = new HBox(5);
//        progressReport.setPrefHeight(10);
//        progressReport.getChildren().setAll(loadProgress);
//        progressReport.getChildren().setAll(loadProgress,loadTimeLabel);
//        progressReport.setAlignment(Pos.BASELINE_CENTER);

//        return progressReport;
    }

    private void subscribe(String chan) {
        try {
            executorService.execute(() -> {
                try {
                    logger.info("开启监听 : " + chan);
                    jedis.subscribe(jedisPubSub, chan);
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
            });
        } catch(Exception ex) {
            logger.error("redis初始化失败",ex);
        } finally {
            logger.info("Redis初始化完成");
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
