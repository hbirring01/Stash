package com.example.creditcardapp.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.repository.OffersRepository
import com.example.creditcardapp.domain.model.Offer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val repository: OffersRepository,
) : ViewModel() {

    val offers: StateFlow<List<Offer>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setActivated(id: Long, activated: Boolean) {
        viewModelScope.launch { repository.setActivated(id, activated) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }
}
