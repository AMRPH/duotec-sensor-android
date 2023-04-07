package com.gelios.configurator.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

import com.gelios.configurator.R;
import com.gelitenight.waveview.library.WaveView;

import java.util.ArrayList;
import java.util.List;

public class WaveHelper {
    private WaveView mWaveView;
    int BEHIND_WAVE_COLOR = Color.parseColor("#77148112");
    int FRONT_WAVE_COLOR = Color.parseColor("#7508B400");
    private AnimatorSet mAnimatorSet;

    public WaveHelper(WaveView waveView) {
        mWaveView = waveView;
        mWaveView.setWaterLevelRatio(0.02f);
        BEHIND_WAVE_COLOR = ContextCompat.getColor(mWaveView.getContext(), R.color.colorFuelBehaind);
        FRONT_WAVE_COLOR = ContextCompat.getColor(mWaveView.getContext(), R.color.colorFuelFront);
        mWaveView.setWaveColor(BEHIND_WAVE_COLOR, FRONT_WAVE_COLOR);
        initAnimation();
    }

    public void start() {
        mWaveView.setShowWave(true);
        if (mAnimatorSet != null) {
            mAnimatorSet.start();
        }
    }

    private void initAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet.end();
        }
        List<Animator> animators = new ArrayList<>();

        // horizontal animation.
        // wave waves infinitely.
        ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(
                mWaveView, "waveShiftRatio", 0f, 1f);
        waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveShiftAnim.setDuration(1000);
        waveShiftAnim.setInterpolator(new LinearInterpolator());
        animators.add(waveShiftAnim);

//        // vertical animation.
//        // water level increases from 0 to center of WaveView
//        ObjectAnimator waterLevelAnim = ObjectAnimator.ofFloat(mWaveView, "waterLevelRatio", 0f, 0.5f);
//        waterLevelAnim.setDuration(10000);
//        waterLevelAnim.setInterpolator(new DecelerateInterpolator());
//        animators.add(waterLevelAnim);

        // amplitude animation.
        // wave grows big then grows small, repeatedly
        ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(
                mWaveView, "amplitudeRatio", 0.0001f, 0.03f);
        amplitudeAnim.setRepeatCount(ValueAnimator.INFINITE);
        amplitudeAnim.setRepeatMode(ValueAnimator.REVERSE);
        amplitudeAnim.setDuration(5000);
        amplitudeAnim.setInterpolator(new LinearInterpolator());
        animators.add(amplitudeAnim);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
    }

    public void setLevel(float level ) {
        ObjectAnimator waterLevelAnim = ObjectAnimator.ofFloat(mWaveView, "waterLevelRatio", mWaveView.getWaterLevelRatio(), level);
        waterLevelAnim.setDuration(2000);
        waterLevelAnim.setInterpolator(new DecelerateInterpolator());
        waterLevelAnim.start();
        mWaveView.setWaterLevelRatio(level);
    }


    public void cancel() {
        if (mAnimatorSet != null) {
//            mAnimatorSet.cancel();
            mAnimatorSet.end();
        }
        mWaveView.setAmplitudeRatio(0.001f);

    }
}