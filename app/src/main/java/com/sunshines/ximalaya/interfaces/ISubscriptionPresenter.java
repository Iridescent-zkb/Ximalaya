package com.sunshines.ximalaya.interfaces;

import com.sunshines.ximalaya.base.IBasePresenter;
import com.ximalaya.ting.android.opensdk.model.album.Album;


public interface ISubscriptionPresenter extends IBasePresenter<ISubscriptionCallback> {
    /**
     * 添加订阅
     * @param album
     */
    void addSubscription(Album album);

    /**
     * 删除订阅
     * @param album
     */
    void deleteSubscription(Album album);

    /**
     * 获取订阅列表
     */
    void getSubscriptionList();

    /**
     * 判断专辑是否已订阅
     * @param album
     */
    boolean isSub(Album album);
}
