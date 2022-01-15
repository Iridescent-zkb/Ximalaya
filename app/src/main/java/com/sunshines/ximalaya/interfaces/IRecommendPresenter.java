package com.sunshines.ximalaya.interfaces;

import com.sunshines.ximalaya.base.IBasePresenter;


public interface IRecommendPresenter extends IBasePresenter<IRecommendViewCallback> {

    /**
     * 获取推荐内容
     */
    void getRecommendList();



}
