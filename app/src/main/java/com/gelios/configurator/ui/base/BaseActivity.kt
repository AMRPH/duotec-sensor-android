package com.gelios.configurator.ui.base

import dagger.android.support.DaggerAppCompatActivity

abstract class BaseActivity : DaggerAppCompatActivity() {
    abstract val TAG: String
}

