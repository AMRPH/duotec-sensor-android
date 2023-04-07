package com.gelios.configurator.ui.base

import android.view.View
import androidx.fragment.app.Fragment
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon

abstract class BaseFragment : Fragment() {
    private val listBalloon = mutableListOf<Balloon>()

    fun showErrorBalloon(view: View, text: String) {
        val balloon = createBalloon(requireContext()) {
            setArrowSize(10)
            setFocusable(false)
            setWidthRatio(0.4f)
            setHeight(65)
            setArrowPosition(0.7f)
            setArrowOrientation(ArrowOrientation.RIGHT)
            setCornerRadius(4f)
            setAlpha(0.9f)
            setText(text)
            setTextSize(18f)
            setTextColorResource(android.R.color.black)
            setBackgroundColorResource(android.R.color.white)
            setBalloonAnimation(BalloonAnimation.FADE)
            setLifecycleOwner(lifecycleOwner)
        }
        balloon.showAlignLeft(view)
        listBalloon.add(balloon)
    }

    fun hideAllBalloon() {
        if (listBalloon.isNotEmpty()) {
            for (balloon in listBalloon) {
                balloon.dismiss()
            }
            listBalloon.clear()
        }
    }
}