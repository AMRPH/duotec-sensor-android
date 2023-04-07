package com.gelios.configurator.di.builder

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import com.gelios.configurator.di.factory.ViewModelFactory

@Module(
    includes = [
        (ViewModelBuilder::class)
    ]
)
abstract class ViewModelFactoryBuilder {

    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}
