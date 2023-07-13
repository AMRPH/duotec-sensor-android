package com.gelios.configurator.di.builder

import dagger.Module
import dagger.android.ContributesAndroidInjector
import com.gelios.configurator.ui.sensor.fuel.fragments.settings.SettingsFuelFragment
import com.gelios.configurator.ui.sensor.fuel.fragments.monitoring.HomeFuelFragment

@Module
abstract class FragmentBuilder {

    @ContributesAndroidInjector
    abstract fun bindHomeFragment(): HomeFuelFragment

    @ContributesAndroidInjector
    abstract fun bindSettingsFragment(): SettingsFuelFragment

}
