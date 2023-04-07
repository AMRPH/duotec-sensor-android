package com.gelios.configurator.di.builder

import dagger.Module
import dagger.android.ContributesAndroidInjector
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.ui.device.fuel.DeviceFuelActivity

@Module
abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = [FragmentBuilder::class])
    abstract fun bindMainActivity(): DeviceFuelActivity

    @ContributesAndroidInjector(modules = [FragmentBuilder::class])
    abstract fun bindChooseDeviceActivity(): ChooseDeviceActivity

}

