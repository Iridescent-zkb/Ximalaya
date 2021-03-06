package com.sunshines.ximalaya;

import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST_LOOP;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_RANDOM;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_SINGLE_LOOP;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.sunshines.ximalaya.adapters.PlayerTrackPageAdapter;
import com.sunshines.ximalaya.base.BaseActivity;
import com.sunshines.ximalaya.interfaces.IPlayerCallback;
import com.sunshines.ximalaya.presenters.PlayerPresenter;
import com.sunshines.ximalaya.utils.LogUtil;
import com.sunshines.ximalaya.views.SobPopWindow;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayerActivity extends BaseActivity implements IPlayerCallback, ViewPager.OnPageChangeListener {

    private static final String TAG = "PlayerActivity";
    private ImageView mControlBtn;
    private PlayerPresenter mPlayerPresenter;
    private SimpleDateFormat mMinFormat = new SimpleDateFormat("mm:ss");
    private SimpleDateFormat mHourFormat = new SimpleDateFormat("hh:mm:ss");
    private TextView mTotalDuration;
    private TextView mCurrentPosition;
    private SeekBar mDurationBar;
    private int mCurrentProgress = 0;
    private boolean mIsUserTouchProgressBar = false;
    private ImageView mPlayNextBtn;
    private ImageView mPlayPreBtn;
    private TextView mTrackTitleTv;
    private String mTrackTitleText;
    private ViewPager mTrackPageView;
    private PlayerTrackPageAdapter mTrackPageAdapter;
    private boolean mIsUserSlidePager = false;
    private ImageView mPlayModeSwitchBtn;
    private XmPlayListControl.PlayMode mCurrentMode = PLAY_MODEL_LIST;
    //
    private static Map<XmPlayListControl.PlayMode, XmPlayListControl.PlayMode> sPlayModeRule = new HashMap<>();

    private static Map<XmPlayListControl.PlayMode, String> sPlayModeStr = new HashMap<>();
    public final int BG_ANIMATION_DURATION = 500;


    // 1.PLAY_MODEL_LIST ????????????
    // 2.PLAY_MODEL_LIST_LOOP ????????????
    // 3.PLAY_MODEL_RANDOM ????????????
    // 4.PLAY_MODEL_SINGLE_LOOP ????????????
    static {
        sPlayModeRule.put(PLAY_MODEL_LIST, PLAY_MODEL_LIST_LOOP);
        sPlayModeRule.put(PLAY_MODEL_LIST_LOOP, PLAY_MODEL_RANDOM);
        sPlayModeRule.put(PLAY_MODEL_RANDOM, PLAY_MODEL_SINGLE_LOOP);
        sPlayModeRule.put(PLAY_MODEL_SINGLE_LOOP, PLAY_MODEL_LIST);
        sPlayModeStr.put(PLAY_MODEL_LIST, "????????????");
        sPlayModeStr.put(PLAY_MODEL_LIST_LOOP, "????????????");
        sPlayModeStr.put(PLAY_MODEL_RANDOM, "????????????");
        sPlayModeStr.put(PLAY_MODEL_SINGLE_LOOP, "????????????");

    }

    private View mPlayListBtn;
    private SobPopWindow mSobPopWindow;
    private ValueAnimator mEnterBgAnimator;
    private ValueAnimator mOutBgAnimator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "onCreate...");
        setContentView(R.layout.activity_player);
        initView();
        mPlayerPresenter = PlayerPresenter.getInstance();
        mPlayerPresenter.registerViewCallback(this);
        initEvent();
        initBgAnimation();
    }

    private void initBgAnimation() {
        mEnterBgAnimator = ValueAnimator.ofFloat(1.0f, 0.7f);
        mEnterBgAnimator.setDuration(BG_ANIMATION_DURATION);
        mEnterBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                // ????????????????????????????????????
                updateBgAlpha(value);
            }
        });
        // ?????????
        mOutBgAnimator = ValueAnimator.ofFloat(0.7f, 1.0f);
        mOutBgAnimator.setDuration(BG_ANIMATION_DURATION);
        mOutBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                updateBgAlpha(value);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy...");
        // ????????????
        if (mPlayerPresenter != null) {
            mPlayerPresenter.unRegisterViewCallback(this);
            mPlayerPresenter = null;
        }
    }


    /**
     * ??????????????????????????????
     */
    private void initEvent() {
        mControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ??????????????????????????????????????????????????????
                if (mPlayerPresenter.isPlaying()) {
                    mPlayerPresenter.pause();
                } else {
                    // ?????????????????????????????????
                    mPlayerPresenter.play();
                }

            }
        });
        mDurationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mCurrentProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsUserTouchProgressBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsUserTouchProgressBar = false;
                //?????????????????????????????????????????????
                mPlayerPresenter.seekTo(mCurrentProgress);
            }
        });
        mPlayPreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //???????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playPre();
                }
            }
        });
        mPlayNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //???????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playNext();
                }
            }
        });

        mTrackPageView.addOnPageChangeListener(this);

        mTrackPageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    // action_down?????? ??????????????????????????????
                    case MotionEvent.ACTION_DOWN:
                        // ??????????????????????????????????????????????????????????????????true
                        mIsUserSlidePager = true;
                        break;
                }
                return false;
            }
        });
        mPlayModeSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPlayMode(true);
            }
        });
        mPlayListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //??????????????????
                mSobPopWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);
                // ????????????????????? ????????????????????????
                mEnterBgAnimator.start();

            }
        });
        mSobPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // pop?????????????????????????????????
                mOutBgAnimator.start();
            }
        });
        mSobPopWindow.setPlayListItemClickListener(new SobPopWindow.PlayListItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // ????????????????????????item????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playByIndex(position);
                }
            }
        });

        mSobPopWindow.setPlayListActionListener(new SobPopWindow.PlayListActionListener() {
            @Override
            public void onPlayModeClick() {
                // ??????????????????
                switchPlayMode(false);
            }

            @Override
            public void onOrderClick() {
                // ???????????????????????????????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.reversePlayList();
                }
            }
        });
    }
    private void switchPlayMode(boolean isOutSideTouched) {
        // ???????????????mode??????????????????mode
        XmPlayListControl.PlayMode playMode = sPlayModeRule.get(mCurrentMode);
        // ??????????????????
        if (mPlayerPresenter != null) {
            mPlayerPresenter.switchPlayMode(playMode);

        }
    }

    /**
     * ???????????????????????????
     *
     * @param alpha
     */
    public void updateBgAlpha(float alpha) {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.alpha = alpha;
        window.setAttributes(attributes);
    }

    /**
     * ????????????????????????????????????????????????
     * PLAY_MODEL_LIST ????????????
     * PLAY_MODEL_LIST_LOOP ????????????
     * PLAY_MODEL_RANDOM ????????????
     * PLAY_MODEL_SINGLE_LOOP ????????????
     */
    private void updatePlayModeBtnImg() {
        int resId = R.drawable.selector_player_mode_list_order;
        switch (mCurrentMode) {
            case PLAY_MODEL_LIST:
                resId = R.drawable.selector_player_mode_list_order;
                break;
            case PLAY_MODEL_LIST_LOOP:
                resId = R.drawable.selector_player_mode_list_order_loop;
                break;
            case PLAY_MODEL_RANDOM:
                resId = R.drawable.selector_player_mode_random;
                break;
            case PLAY_MODEL_SINGLE_LOOP:
                resId = R.drawable.selector_player_mode_single_loop;
                break;
        }
        mPlayModeSwitchBtn.setImageResource(resId);
    }

    /**
     * ??????????????????
     */
    private void initView() {

        mControlBtn = this.findViewById(R.id.play_or_pause_btn);
        mTotalDuration = this.findViewById(R.id.track_duration);
        mCurrentPosition = this.findViewById(R.id.current_position);
        mDurationBar = this.findViewById(R.id.track_seek_bar);
        mPlayNextBtn = this.findViewById(R.id.play_next);
        mPlayPreBtn = this.findViewById(R.id.play_pre);
        mTrackTitleTv = this.findViewById(R.id.track_title);
        if (!TextUtils.isEmpty(mTrackTitleText)) {
            mTrackTitleTv.setText(mTrackTitleText);
        }
        mTrackPageView = this.findViewById(R.id.track_pager_view);
        // ???????????????
        mTrackPageAdapter = new PlayerTrackPageAdapter();
        // ???????????????
        mTrackPageView.setAdapter(mTrackPageAdapter);
        // ???????????????????????????
        mPlayModeSwitchBtn = this.findViewById(R.id.player_mode_switch_btn);
        // ????????????
        mPlayListBtn = this.findViewById(R.id.player_list);
        mSobPopWindow = new SobPopWindow();

    }

    @Override
    public void onPlayStart() {
        // ????????????,??????UI??????????????????
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_pause);
        }
    }

    @Override
    public void onPlayPause() {
        // ?????????????????????UI??????????????????
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayStop() {
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_play);
        }
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
//        LogUtil.d(TAG,"list  -->" + list);
        // ????????????????????????????????????
        if (mTrackPageAdapter != null) {
            mTrackPageAdapter.setData(list);
        }
        // ????????????????????????????????????????????????
        if (mSobPopWindow != null) {
            mSobPopWindow.setListData(list);
        }

    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode mode) {
        // ?????????????????????????????????UI
        mCurrentMode = mode;
        // ??????pop??????????????????
        mSobPopWindow.updatePlayMode(mCurrentMode);
        updatePlayModeBtnImg();

    }

    @Override
    public void onProgressChange(int currentDuration, int total) {
        mDurationBar.setMax(total);
        // ??????????????????(???????????????)
        String totalDuration;
        String currentPosition;
        if (total > 1000 * 60 * 60) {
            totalDuration = mHourFormat.format(total);
            currentPosition = mHourFormat.format(currentDuration);
        } else {
            totalDuration = mMinFormat.format(total);
            currentPosition = mMinFormat.format(currentDuration);
        }
        if (mTotalDuration != null) {
            mTotalDuration.setText(totalDuration);
        }
        // ??????????????????
        if (mCurrentPosition != null) {
            mCurrentPosition.setText(currentPosition);
        }
        // ????????????
        // ??????????????????
        if (!mIsUserTouchProgressBar) {
            mDurationBar.setProgress(currentDuration);
        }
    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int playIndex) {
        if (track == null) {
            LogUtil.d(TAG,"onTrackUpdate --> track null");
            return;
        }
        this.mTrackTitleText = track.getTrackTitle();
        if (mTrackTitleTv != null) {
            // ???????????????????????????
            mTrackTitleTv.setText(mTrackTitleText);
        }
        // ????????????????????????????????????????????????????????????????????????
        // ??????????????????????????????
        if (mTrackPageView != null) {
            mTrackPageView.setCurrentItem(playIndex, true);
        }
        // ??????????????????????????????
        if (mSobPopWindow != null) {
            mSobPopWindow.setCurrentPlayPosition(playIndex);
        }

    }

    @Override
    public void updateListOrder(boolean isReverse) {
        mSobPopWindow.updateOrderIcon(isReverse);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        LogUtil.d(TAG, "position -- >" + position);
        // ??????????????????????????????????????????????????????
        // ????????????????????????????????????????????????????????????????????????????????????
        // ????????????????????????????????????????????????????????????????????????????????????????????????
        if (mPlayerPresenter != null && mIsUserSlidePager) {
            mPlayerPresenter.playByIndex(position);
        }
        mIsUserSlidePager = false;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
