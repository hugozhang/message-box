package com.winning.hmap.service;

import com.winning.hmap.util.Base64Utils;
import com.winning.hmap.vo.ConfigInfo;
import com.winning.hmap.vo.LoginResponse;
import com.winning.hmap.vo.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit2Holder
 *
 * @author: hugo.zxh
 * @date: 2023/09/12 15:55
 */
public class Retrofit2Holder {

    private static final Integer connectTimeout = 5;

    private static final Integer writeTimeout = 5;

    private static final Integer readTimeout = 5;

    private static Retrofit retrofit;

    public static String setCookieHeader;

    /**
     * host url
     * @param url
     */
    public static void init(String url) {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Request.Builder builder = request.newBuilder();
                    if (setCookieHeader != null) {
                        builder.addHeader("Cookie",setCookieHeader);
                    }
                    Response response = chain.proceed(builder.build());
                    String path = request.url().url().getPath();
                    if ("/rest/hmap/auth/login".contains(path)) {
                        String header = response.header("Set-Cookie");
                        if (header != null || header.trim().length() != 0) {
                            setCookieHeader = header;
                        }
                    }
                    return response;
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addCallAdapterFactory(Retrofit2AdapterFactory.create())
                .addConverterFactory(FastJsonConverterFactory.create())
                .build();
    }

    public static RpcService getService() {
        return retrofit.create(RpcService.class);
    }

    public static void main(String[] args) {
        init("http://192.168.18.90:1315");
        RpcService service = getService();
        LoginResponse login = service.login("C0518", Base64Utils.encodeToString("123456"), "T001");
        System.out.println(login);
        R<ConfigInfo> resp = service.getConfigInfo();
        System.out.println(resp.getData());
    }

}
