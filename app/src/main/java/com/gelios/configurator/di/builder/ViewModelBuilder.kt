package com.gelios.configurator.di.builder

import androidx.lifecycle.ViewModel
import com.gelios.configurator.di.qualifier.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import com.gelios.configurator.ui.choose.ChooseDeviceViewModel

@Module
abstract class ViewModelBuilder {

    @Binds
    @IntoMap
    @ViewModelKey(ChooseDeviceViewModel::class)
    abstract fun bindChooseDeviceViewModel(chooseDeviceViewModel: ChooseDeviceViewModel): ViewModel
}
