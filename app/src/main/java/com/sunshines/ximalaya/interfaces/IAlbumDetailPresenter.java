package com.sunshines.ximalaya.interfaces;

import com.sunshines.ximalaya.base.IBasePresenter;


public interface IAlbumDetailPresenter extends IBasePresenter<IAlbumDetailViewCallback> {

    /**
     * 下拉刷新加载更多内容
     */
    void pull2RefreshMore();
    /**
     *  上拉加载更多
     */
    void loadMore();

    /**
     * 获取专辑详情
     * @param albumId
     * @param page
     */
    void getAlbumDetail(int albumId,int page);

}
