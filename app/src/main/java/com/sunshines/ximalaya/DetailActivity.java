package com.sunshines.ximalaya;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.header.bezierlayout.BezierLayout;
import com.sunshines.ximalaya.adapters.TrackListAdapter;
import com.sunshines.ximalaya.base.BaseActivity;
import com.sunshines.ximalaya.base.BaseApplication;
import com.sunshines.ximalaya.interfaces.IAlbumDetailViewCallback;
import com.sunshines.ximalaya.interfaces.IPlayerCallback;
import com.sunshines.ximalaya.interfaces.ISubscriptionCallback;
import com.sunshines.ximalaya.interfaces.ISubscriptionPresenter;
import com.sunshines.ximalaya.presenters.AlbumDetailPresenter;
import com.sunshines.ximalaya.presenters.PlayerPresenter;
import com.sunshines.ximalaya.presenters.SubscriptionPresenter;
import com.sunshines.ximalaya.utils.Constants;
import com.sunshines.ximalaya.utils.ImageBlur;
import com.sunshines.ximalaya.utils.LogUtil;
import com.sunshines.ximalaya.views.RoundRectImageView;
import com.sunshines.ximalaya.views.UILoader;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.List;


public class DetailActivity extends BaseActivity implements IAlbumDetailViewCallback, UILoader.OnRetryClickListener, TrackListAdapter.ItemClickListener, IPlayerCallback, ISubscriptionCallback {

    private static final String TAG = "DetailActivity";
    private ImageView mLargeCover;
    private RoundRectImageView mSmallCover;
    private TextView mAlbumTitle;
    private TextView mAlbumAuthor;
    private AlbumDetailPresenter mAlbumDetailPresenter;
    private int mCurrentPage = 1;
    private RecyclerView mDetailList;
    private TrackListAdapter mTrackListAdapter;
    private FrameLayout mDetailListContainer;
    private UILoader mUiLoader;
    private long mCurrentId = -1;
    private ImageView mPlayControlBtn;
    private TextView mPlayControlTips;
    private PlayerPresenter mPlayerPresenter;
    private List<Track> mCurrentTracks = null;
    private final static int DEFAULT_PLAY_INDEX = 0;
    private TwinklingRefreshLayout mRefreshLayout;
    private String mCurrentTrackTitle;
    private TextView mSubBtn;
    private ISubscriptionPresenter mSubscriptionPresenter;
    private Album mCurrentAlbum = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        // ????????????,???????????????
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        initView();
        initPresenter();
        // ???????????????????????????
        updateSubState();
        updatePlayState(mPlayerPresenter.isPlaying());
        initListener();

    }

    private void updateSubState() {
        if (mSubscriptionPresenter != null) {
            boolean isSub = mSubscriptionPresenter.isSub(mCurrentAlbum);
            LogUtil.d(TAG,"isSub -->" + isSub);
            mSubBtn.setText(isSub?R.string.cancel_sub_tips_text:R.string.sub_tips_text);
        }
    }

    private void initPresenter() {
        // ????????????????????????presenter
        mAlbumDetailPresenter = AlbumDetailPresenter.getInstance();
        mAlbumDetailPresenter.registerViewCallback(this);
        // ????????????presenter
        mPlayerPresenter = PlayerPresenter.getInstance();
        mPlayerPresenter.registerViewCallback(this);
        // ???????????????presenter
        mSubscriptionPresenter = SubscriptionPresenter.getInstance();
        mSubscriptionPresenter.registerViewCallback(this);
        mSubscriptionPresenter.getSubscriptionList();
    }


    private void initListener() {
        mPlayControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerPresenter != null) {
                    // ????????????????????????????????????
                    boolean has = mPlayerPresenter.hasPlayList();
                    if (has) {
                        // ????????????????????????
                        handlePlayControl();
                    } else {
                        // ??????????????????
                        handleNoPlayList();
                    }

                }

            }
        });
        mSubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSubscriptionPresenter != null) {
                    boolean isSub = mSubscriptionPresenter.isSub(mCurrentAlbum);
                    // ?????????????????????????????????????????????????????????????????????
                    if (isSub) {
                        mSubscriptionPresenter.deleteSubscription(mCurrentAlbum);
                    }else{
                        mSubscriptionPresenter.addSubscription(mCurrentAlbum);
                    }
                }
            }
        });
    }

    /**
     * ????????????????????????????????????????????????
     */
    private void handleNoPlayList() {
        mPlayerPresenter.setPlayList(mCurrentTracks, DEFAULT_PLAY_INDEX);
    }

    private void handlePlayControl() {
        if (mPlayerPresenter.isPlaying()) {
            // ???????????? ????????????
            mPlayerPresenter.pause();
        } else {
            mPlayerPresenter.play();
        }
    }

    private void initView() {
        mDetailListContainer = this.findViewById(R.id.detail_list_container);
        //
        if (mUiLoader == null) {
            mUiLoader = new UILoader(DetailActivity.this) {
                @Override
                protected View getSuccessView(ViewGroup container) {
                    return createSuccessView(container);
                }
            };
            mDetailListContainer.removeAllViews();
            mDetailListContainer.addView(mUiLoader);
            mUiLoader.setOnRetryClickListener(this);
        }
        mLargeCover = this.findViewById(R.id.iv_large_cover);
        mSmallCover = this.findViewById(R.id.iv_small_cover);
        mAlbumTitle = this.findViewById(R.id.tv_album_title);
        mAlbumAuthor = this.findViewById(R.id.tv_album_author);
        // ?????????????????????
        mPlayControlBtn = this.findViewById(R.id.detail_play_control);
        mPlayControlTips = this.findViewById(R.id.play_control_tv);
        // ????????????????????????????????????
        mPlayControlTips.setSelected(true);

        mSubBtn = this.findViewById(R.id.detail_sub_btn);


    }

    private boolean mIsLoaderMore = false;

    private View createSuccessView(ViewGroup container) {
        View detailListView = LayoutInflater.from(this).inflate(R.layout.item_detail_list, container, false);
        mDetailList = detailListView.findViewById(R.id.album_detail_list);
        mRefreshLayout = detailListView.findViewById(R.id.refresh_layout);
        // ??????RecyclerView
        // 1.?????????????????????
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mDetailList.setLayoutManager(layoutManager);
        // 2.???????????????
        mTrackListAdapter = new TrackListAdapter();
        mDetailList.setAdapter(mTrackListAdapter);
        // ??????item???????????????
        mDetailList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = UIUtil.dip2px(view.getContext(), 2);
                outRect.bottom = UIUtil.dip2px(view.getContext(), 2);
                outRect.left = UIUtil.dip2px(view.getContext(), 2);
                outRect.right = UIUtil.dip2px(view.getContext(), 2);
            }
        });
        mTrackListAdapter.setItemClickListener(this);
        BezierLayout headerView = new BezierLayout(this);
        mRefreshLayout.setHeaderView(headerView);
        mRefreshLayout.setMaxHeadHeight(140f);
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(TwinklingRefreshLayout refreshLayout) {
                super.onRefresh(refreshLayout);
                Toast.makeText(DetailActivity.this, "??????????????????...", Toast.LENGTH_SHORT).show();
                BaseApplication.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DetailActivity.this, "????????????...", Toast.LENGTH_SHORT).show();
                        mRefreshLayout.finishRefreshing();
                    }
                }, 2000);
            }

            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                super.onLoadMore(refreshLayout);
//                ToastUtil.showShort(DetailActivity.this,"??????????????????...");
                //????????????????????????
                if (mAlbumDetailPresenter != null) {
                    mAlbumDetailPresenter.loadMore();
                    mIsLoaderMore = true;
                }
            }
        });
        return detailListView;
    }

    @Override
    public void onDetailListLoaded(List<Track> tracks) {
        if (mIsLoaderMore && mRefreshLayout != null) {
            mRefreshLayout.finishLoadmore();
            mIsLoaderMore = false;
        }
        this.mCurrentTracks = tracks;
        // ???????????????????????????????????????UI??????
        if (tracks == null || tracks.size() == 0) {
            if (mUiLoader != null) {
                mUiLoader.updateStatus(UILoader.UIStatus.EMPTY);
            }
        }
        if (mUiLoader != null) {
            mUiLoader.updateStatus(UILoader.UIStatus.SUCCESS);
        }
        // ??????/??????UI??????
        mTrackListAdapter.setData(tracks);
    }

    @Override
    public void onNetworkError(int errorCode, String errorMsg) {
        // ?????????????????????????????????????????????
        mUiLoader.updateStatus(UILoader.UIStatus.NETWORK_ERROR);
    }

    @Override
    public void onAlbumLoaded(Album album) {
        this.mCurrentAlbum = album;
        // ???????????????????????????
        long id = album.getId();
        LogUtil.d(TAG, "albumId -->" + id);
        mCurrentId = id;
        if (mAlbumDetailPresenter != null) {
            mAlbumDetailPresenter.getAlbumDetail((int) id, mCurrentPage);
        }
        // ?????????,??????loading??????
        if (mUiLoader != null) {
            mUiLoader.updateStatus(UILoader.UIStatus.LOADING);
        }
        if (mAlbumTitle != null) {
            mAlbumTitle.setText(album.getAlbumTitle());
        }
        if (mAlbumAuthor != null) {
            mAlbumAuthor.setText(album.getAnnouncer().getNickname());
        }
        // ??????????????????
        if (mLargeCover != null && null != mLargeCover) {
//            Picasso.get().load(album.getCoverUrlLarge()).into(mLargeCover, new Callback() {
//                @Override
//                public void onSuccess() {
//                    Drawable drawable = mLargeCover.getDrawable();
//                    if (drawable != null) {
//                        // ???????????????????????????
//                        ImageBlur.makeBlur(mLargeCover, DetailActivity.this);
//                    }
//                }
//
//                @Override
//                public void onError(Exception e) {
//                    LogUtil.d(TAG, "onError ...");
//                }
//            });
            Glide.with(this)
                    .load(album.getCoverUrlLarge())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            BaseApplication.getHandler().post(() -> ImageBlur.makeBlur(DetailActivity.this, resource, mLargeCover));
                            return true;
                        }
                    }).submit();
        }

        if (mSmallCover != null) {
            Glide.with(this).load(album.getCoverUrlLarge()).into(mSmallCover);
        }
    }

    @Override
    public void onLoaderMoreFinished(int size) {
        if (size > 0) {
            Toast.makeText(this, "????????????" + size + "?????????", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRefreshFinished(int size) {

    }

    @Override
    public void onRetryClick() {
        // ?????????????????????????????????????????????????????????????????????
        if (mAlbumDetailPresenter != null) {
            mAlbumDetailPresenter.getAlbumDetail((int) mCurrentId, mCurrentPage);
        }
    }

    @Override
    public void onItemClick(List<Track> detailData, int position) {
        // ????????????????????????
        PlayerPresenter playerPresenter = PlayerPresenter.getInstance();
        playerPresenter.setPlayList(detailData, position);
        // ????????????????????????
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ????????????
        if (mAlbumDetailPresenter != null){
            mAlbumDetailPresenter.unRegisterViewCallback(this);
            mAlbumDetailPresenter = null;
        }
        if (mPlayerPresenter != null) {
            mPlayerPresenter.unRegisterViewCallback(this);
            mPlayerPresenter = null;
        }
        if (mSubscriptionPresenter != null) {
            mSubscriptionPresenter.unRegisterViewCallback(this);
            mSubscriptionPresenter = null;
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param playing
     */
    private void updatePlayState(boolean playing) {
        // ???????????????????????????????????????????????????
        if (mPlayControlBtn != null && mPlayControlTips != null) {
            mPlayControlBtn.setImageResource(playing ? R.drawable.selector_play_control_pause : R.drawable.selector_play_control_play);
            if (!playing) {
                // ???????????????????????????????????????
                mPlayControlTips.setText(R.string.click_play_tips_text);
            } else {
                // ????????????????????????????????????????????????
                if (!TextUtils.isEmpty(mCurrentTrackTitle)) {
                    mPlayControlTips.setText(mCurrentTrackTitle);
                }
            }
        }
    }

    /**
     * ???????????????
     */
    @Override
    public void onPlayStart() {
        // ????????????????????????,??????????????????????????????
        updatePlayState(true);
    }

    /**
     * ???????????????
     */
    @Override
    public void onPlayPause() {
        // ??????????????????????????????????????????????????????
        updatePlayState(false);
    }

    /**
     * ?????????????????????
     */
    @Override
    public void onPlayStop() {
        updatePlayState(false);
    }

    @Override
    public void onPlayError() {

    }

    @Override
    public void onNextPlay(Track track) {

    }

    @Override
    public void onPrePlay(Track track) {

    }

    @Override
    public void onListLoaded(List<Track> list) {

    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode mode) {

    }

    @Override
    public void onProgressChange(int currentProgress, int total) {

    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int playIndex) {
        if (track != null) {
            mCurrentTrackTitle = track.getTrackTitle();
            if (!TextUtils.isEmpty(mCurrentTrackTitle) && mPlayControlTips != null) {
                mPlayControlTips.setText(mCurrentTrackTitle);
            }
        }
    }

    @Override
    public void updateListOrder(boolean isReverse) {

    }

    @Override
    public void onAddResult(boolean isSuccess) {
        // ??????UI
        if (isSuccess) {
            // ??????????????????????????????UI???????????????
            mSubBtn.setText(R.string.cancel_sub_tips_text);
        }
        // ????????????
        String tipsText = isSuccess?"????????????":"????????????";
        Toast.makeText(this, tipsText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteResult(boolean isDeleted) {
        // ??????UI
        if (isDeleted) {
            // ??????????????????????????????UI???????????????
            mSubBtn.setText(R.string.sub_tips_text);
        }
        // ????????????
        String tipsText = isDeleted?"????????????":"????????????";
        Toast.makeText(this, tipsText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubscriptionsLoaded(List<Album> albums) {
        // ????????????????????????
    }

    @Override
    public void onSubFull() {
        // ?????????????????????,??????????????????
        Toast.makeText(this, "????????????????????????" + Constants.MAX_SUB_COUNT, Toast.LENGTH_SHORT).show();
    }
}
