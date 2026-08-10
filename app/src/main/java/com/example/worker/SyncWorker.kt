package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log
import com.example.data.db.PulseDatabase
import com.example.data.models.MessageStatus

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting offline data synchronization...")
            
            val database = PulseDatabase.getDatabase(applicationContext)
            
            // 1. Query PulseDatabase for unsynced messages
            val pendingMessages = database.messageDao().getPendingMessages()
            
            if (pendingMessages.isNotEmpty()) {
                Log.d("SyncWorker", "Found ${pendingMessages.size} pending messages to sync.")
                
                // 2. Iterate through them and push to network (Simulated)
                for (message in pendingMessages) {
                    // 3. Mark them as synced in the local database
                    database.messageDao().updateMessageStatus(message.id, MessageStatus.SENT.name)
                }
                
                Log.d("SyncWorker", "Offline data synchronization completed successfully.")
            } else {
                Log.d("SyncWorker", "No offline data to synchronize.")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error synchronizing offline data", e)
            Result.retry()
        }
    }
}
