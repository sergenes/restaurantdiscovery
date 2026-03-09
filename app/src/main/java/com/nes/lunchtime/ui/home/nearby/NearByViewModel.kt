package com.nes.lunchtime.ui.home.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.domain.GetRestaurantsUseCase
import com.nes.lunchtime.domain.Restaurant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NearByViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data object Refreshing : UiState()
        data class Success(val restaurants: List<Restaurant>) : UiState()
        data class Error(val message: String, val canRetry: Boolean = true) : UiState()
    }

    private sealed interface TriggerType {
        data object Initial : TriggerType
        data object Retry : TriggerType
        data object Refresh : TriggerType
    }

    private val _location = MutableStateFlow<LatLng?>(null)

    // No replay — stale retry/refresh from a previous location must not bleed into a new one.
    // onStart handles the initial trigger each time flatMapLatest opens a new inner flow.
    private val _trigger = MutableSharedFlow<TriggerType>(extraBufferCapacity = 1)

    val uiState: StateFlow<UiState> = _location
        .filterNotNull()
        .flatMapLatest { location ->
            _trigger
                .onStart { emit(TriggerType.Initial) }   // auto-trigger on first subscription
                .transformLatest { trigger ->
                    emit(if (trigger == TriggerType.Refresh) UiState.Refreshing else UiState.Loading)

                    val result = runCatching { getRestaurantsUseCase.getNearby(location) }
                        .getOrElse { Result.failure(it) }

                    emit(
                        if (result.isSuccess) {
                            UiState.Success(result.getOrThrow())
                        } else {
                            val error = result.exceptionOrNull()!!
                            UiState.Error(
                                message = when (error) {
                                    is IOException -> "Network error — check your connection"
                                    else -> error.localizedMessage ?: "Something went wrong"
                                }
                            )
                        }
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun setLocation(location: LatLng) {
        _location.value = location   // StateFlow deduplicates — no reload on same location
    }

    fun retry() {
        _trigger.tryEmit(TriggerType.Retry)
    }

    fun refresh() {
        _trigger.tryEmit(TriggerType.Refresh)
    }
}
