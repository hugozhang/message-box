package com.winning.hmap.service;

import com.winning.hmap.vo.*;
import retrofit2.http.*;

/**
 * 远程服务
 *
 * @author: hugo.zxh
 * @date: 2023/09/12 15:49
 */
public interface RpcService {

    @Headers("X-Header: MessageBox")
    @FormUrlEncoded
    @POST("/rest/hmap/auth/login")
    LoginResponse login(@Field("yh_mc") String username, @Field("yh_mm") String password, @Field("zhid") String tenantCode);

    @POST("/rest/hmap/auth/logout")
    LogoutResponse loginOut();



    @GET("/hmap/portal/getConfigInfo")
    R<ConfigInfo> getConfigInfo();

    @POST("hmap/msgbox/getVerInfo")
    R<VerInfo> getVerInfo();

}
