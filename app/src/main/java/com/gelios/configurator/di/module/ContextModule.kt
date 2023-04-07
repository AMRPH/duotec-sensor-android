package com.gelios.configurator.di.module

import android.app.Application
import android.content.Context
import com.gelios.configurator.di.builder.ViewModelFactoryBuilder
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    includes = [
        ViewModelFactoryBuilder::class
    ]
)
class ContextModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application
    }

}
