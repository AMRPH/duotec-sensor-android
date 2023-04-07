package com.gelios.configurator.di.component

import android.app.Application
import com.gelios.configurator.di.builder.ActivityBuilder
import com.gelios.configurator.di.module.ContextModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import com.gelios.configurator.ui.App
import javax.inject.Singleton


@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ContextModule::class,
        ActivityBuilder::class
    ]
)

interface AppComponent : AndroidInjector<App> {

    @Suppress("UNCHECKED_CAST")
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent

    }

}