package com.kieronquinn.app.simnumbersetter.ui.screens.main

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.simnumbersetter.R
import com.kieronquinn.app.simnumbersetter.repositories.PermissionRepository
import com.kieronquinn.app.simnumbersetter.repositories.RootRepository
import com.kieronquinn.app.simnumbersetter.repositories.ServiceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MainViewModel : ViewModel() {

    abstract val state: StateFlow<State>
    abstract fun onNumberChanged(newNumber: String)
    abstract fun onSaveClicked()
    abstract fun onMenuItemClicked(context: Context, id: Int)

    sealed class State {
        data class Loading(val loadType: LoadType) : State()
        data class Loaded(val number: String, val editableNumber: String) : State()
        data class Error(val errorType: ErrorType) : State()
    }

    enum class ErrorType(@StringRes val messageRes: Int) {
        NO_ROOT(R.string.error_no_root),
        NO_PERMISSION(R.string.error_no_permission),
        NO_XPOSED(R.string.error_no_xposed),
        SAVE_FAILED(R.string.error_save_failed)
    }

    enum class LoadType(@StringRes val contentRes: Int) {
        LOADING(R.string.loading_loading),
        SAVING(R.string.loading_saving)
    }

}

class MainViewModelImpl(
    private val rootRepository: RootRepository,
    private val permissionRepository: PermissionRepository,
    private val serviceRepository: ServiceRepository
) : MainViewModel() {

    companion object {
        private const val URL_GITHUB = "https://kieronquinn.co.uk/redirect/SNS/github"
        private const val URL_DONATE = "https://kieronquinn.co.uk/redirect/SNS/donate"
    }

    private val _state = MutableStateFlow<State>(State.Loading(LoadType.LOADING))
    override val state: StateFlow<State> = _state.asStateFlow()

    override fun onNumberChanged(newNumber: String) {
        _state.update { current ->
            if (current is State.Loaded) {
                current.copy(editableNumber = newNumber)
            } else {
                current
            }
        }
    }

    override fun onSaveClicked() {
        val current = _state.value
        val number = when (current) {
            is State.Loaded -> current.editableNumber
            else -> ""
        }
        save(number)
    }

    override fun onMenuItemClicked(context: Context, id: Int) {
        val url = when(id) {
            R.id.menu_github -> URL_GITHUB
            R.id.menu_donate -> URL_DONATE
            else -> return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }catch (e: ActivityNotFoundException) {
            //Nothing can be done
        }
    }

    private fun load() = viewModelScope.launch {
        _state.value = State.Loading(LoadType.LOADING)
        val rooted = try {
            rootRepository.isRooted()
        } catch (e: Exception) {
            false
        }
        if (!rooted) {
            _state.value = State.Error(ErrorType.NO_ROOT)
            return@launch
        }
        val granted = try {
            permissionRepository.grantDumpPermission()
        } catch (e: Exception) {
            false
        }
        if (!granted) {
            _state.value = State.Error(ErrorType.NO_PERMISSION)
            return@launch
        }
        val number = try {
            serviceRepository.runWithService {
                it.line1Number
            }
        } catch (e: Exception) {
            null
        } ?: run {
            _state.value = State.Error(ErrorType.NO_XPOSED)
            return@launch
        }
        _state.value = State.Loaded(
            number = number,
            editableNumber = number
        )
    }

    private fun save(number: String) = viewModelScope.launch {
        _state.value = State.Loading(LoadType.SAVING)
        val result = try {
            serviceRepository.runWithService {
                it.setLine1Number(number, null)
            }
        } catch (e: Exception) {
            null
        } ?: run {
            _state.value = State.Error(ErrorType.NO_XPOSED)
            return@launch
        }
        delay(1000L)
        if (result) {
            load()
        } else {
            _state.value = State.Error(ErrorType.SAVE_FAILED)
        }
    }

    init {
        load()
    }

    override fun onCleared() {
        serviceRepository.unbindServiceIfNeeded()
        super.onCleared()
    }

}
