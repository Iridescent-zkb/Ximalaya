package com.sunshines.ximalaya.presenters;

import com.sunshines.ximalaya.base.BaseApplication;
import com.sunshines.ximalaya.data.HistoryDao;
import com.sunshines.ximalaya.data.IHistoryDao;
import com.sunshines.ximalaya.data.IHistoryDaoCallback;
import com.sunshines.ximalaya.interfaces.IHistoryCallback;
import com.sunshines.ximalaya.interfaces.IHistoryPresenter;
import com.sunshines.ximalaya.utils.Constants;
import com.sunshines.ximalaya.utils.LogUtil;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class HistoryPresenter implements IHistoryPresenter, IHistoryDaoCallback {

    private static final String TAG = "HistoryPresenter";
    private List<IHistoryCallback> mCallbacks = new ArrayList<>();


    private final IHistoryDao mHistoryDao;
    private List<Track> mCurrentHistories = null;
    private Track mCurrentAddTrack = null;

    private HistoryPresenter() {
        mHistoryDao = new HistoryDao();
        mHistoryDao.setCallback(this);
        listHistories();
    }

    private static HistoryPresenter sHistoryPresenter = null;

    public static HistoryPresenter getInstance() {
        if (sHistoryPresenter == null) {
            synchronized (HistoryPresenter.class) {
                if (sHistoryPresenter == null) {
                    sHistoryPresenter = new HistoryPresenter();
                }
            }
        }
        return sHistoryPresenter;
    }

    @Override
    public void listHistories() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.listHistories();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private boolean isDoDelAsOutOfSize = false;

    @Override
    public void addHistory(final Track track) {
        // 需要去判断是否当前历史记录已经超过100条了
        if (mCurrentHistories != null && mCurrentHistories.size() >= Constants.MAX_HISTORY_COUNT) {
            isDoDelAsOutOfSize = true;
            //先要删除最早加入的一条记录,再添加
            this.mCurrentAddTrack = track;
            delHistory(mCurrentHistories.get(mCurrentHistories.size() - 1));
        } else {
            // 否则直接添加
            doAddHistory(track);
        }
    }

    private void doAddHistory(final Track track) {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.addHistory(track);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void delHistory(final Track track) {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.delHistory(track);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void cleanHistories() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.clearHistory();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void registerViewCallback(IHistoryCallback iHistoryCallback) {
        // UI注册过来的
        if (!mCallbacks.contains(iHistoryCallback)) {
            mCallbacks.add(iHistoryCallback);
        }
    }

    @Override
    public void unRegisterViewCallback(IHistoryCallback iHistoryCallback) {
        // 删除UI的回调
        mCallbacks.remove(iHistoryCallback);
    }

    @Override
    public void onHistoryAdd(boolean isSuccess) {
        // nothing to do
        listHistories();
    }

    @Override
    public void onHistoryDel(boolean isSuccess) {
        if (isDoDelAsOutOfSize && mCurrentAddTrack != null) {
            // 添加当前的数据进到数据库内
            isDoDelAsOutOfSize = false;
            addHistory(mCurrentAddTrack);
        } else {
            listHistories();
        }
    }

    @Override
    public void onHistoriesLoaded(final List<Track> tracks) {
        this.mCurrentHistories = tracks;
        LogUtil.d(TAG,"tracks size -->" + tracks.size());
        // 通知UI更新数据
        BaseApplication.getHandler().post(new Runnable() {
            @Override
            public void run() {
                for (IHistoryCallback callback : mCallbacks) {
                    callback.onHistoriesLoaded(tracks);
                }
            }
        });
    }

    @Override
    public void onHistoryClean(boolean isSuccess) {
        listHistories();
    }
}
