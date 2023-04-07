package com.gelios.configurator.di.executor

import io.reactivex.Scheduler

interface PostExecutionThread {

    fun getScheduler(): Scheduler

}