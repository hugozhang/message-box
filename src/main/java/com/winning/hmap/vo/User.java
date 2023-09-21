package com.winning.hmap.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 登陆返回用户
 *
 * @author: hugo.zxh
 * @date: 2023/09/12 16:05
 */
public class User {

    @JSONField(name = "yh_id")
    private Integer yhId;

    /**
     * 医生id
     */
    private String ysid;

    /**
     * 姓名
     */
    private String xm;

    /**
     * 医疗机构代码
     */
    private String mryljgdm;

    /**
     * 医疗机构名称
     */
    private String mryljgmc;

    public String getYsid() {
        return ysid;
    }

    public void setYsid(String ysid) {
        this.ysid = ysid;
    }

    public Integer getYhId() {
        return yhId;
    }

    public void setYhId(Integer yhId) {
        this.yhId = yhId;
    }

    public String getMryljgdm() {
        return mryljgdm;
    }

    public void setMryljgdm(String mryljgdm) {
        this.mryljgdm = mryljgdm;
    }

    public String getMryljgmc() {
        return mryljgmc;
    }

    public void setMryljgmc(String mryljgmc) {
        this.mryljgmc = mryljgmc;
    }

    public String getXm() {
        return xm;
    }

    public void setXm(String xm) {
        this.xm = xm;
    }

}
