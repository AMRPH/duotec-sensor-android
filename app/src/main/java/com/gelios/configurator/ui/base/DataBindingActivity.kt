package com.gelios.configurator.ui.base

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.BR
import javax.inject.Inject

abstract class DataBindingActivity<TViewModel : BaseViewModel, TBinding : ViewDataBinding> :
    BaseActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected lateinit var binding: TBinding

    protected lateinit var viewModel: TViewModel

    abstract fun provideViewModel(): TViewModel
    abstract fun provideLayoutId(): Int

    open fun provideProgressBar(): ProgressBar? {
        return null
    }

    abstract fun provideLifecycleOwner(): DataBindingActivity<TViewModel, TBinding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = provideViewModel()
        binding = DataBindingUtil.setContentView(this, provideLayoutId())
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = provideLifecycleOwner()
    }

    fun showMessage(text: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, duration).show()
    }

    fun showMessage(textId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, textId, duration).show()
    }

    protected fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}
