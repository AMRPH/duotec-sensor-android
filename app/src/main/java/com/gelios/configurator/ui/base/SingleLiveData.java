package com.gelios.configurator.ui.base;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

public class SingleLiveData<T> extends MutableLiveData<T> {
    private AtomicBoolean mPending = new AtomicBoolean(false);
    private T content = null;

    @MainThread
    @Override
    public void observe(final @NonNull LifecycleOwner owner, final @NonNull Observer<? super T> observer) {
        if (hasActiveObservers()) {
//            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
        }

        super.observe(owner, t -> {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(T value) {
        mPending.set(true);
        content = value;
        super.setValue(value);
    }

    @MainThread
    void call() {
        setValue(null);
    }

    public T getContent() {
        return content;
    }
}
