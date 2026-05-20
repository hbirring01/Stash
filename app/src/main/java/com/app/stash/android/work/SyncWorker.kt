package com.app.stash.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.stash.android.data.repository.CreditCardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: CreditCardRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outcome = repository.refreshFromRemote()
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val NAME = "credit-card-sync"
    }
}
