package com.kieronquinn.app.simnumbersetter.repositories

import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import com.kieronquinn.app.simnumbersetter.BuildConfig
import com.kieronquinn.app.simnumbersetter.repositories.PermissionRepository.Companion.PERMISSION_DUMP
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PermissionRepository {

    companion object {
        internal const val PERMISSION_DUMP = "android.permission.DUMP"
    }

    suspend fun grantDumpPermission(): Boolean

}

class PermissionRepositoryImpl(
    private val context: Context,
    private val rootRepository: RootRepository
): PermissionRepository {

    private fun hasDumpPermission(): Boolean {
        return context.packageManager.checkPermission(
            PERMISSION_DUMP, BuildConfig.APPLICATION_ID
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun runGrantCommand(): Shell.Result {
        val userId = Process.myUid() / 100000 // PER_USER_RANGE; UserHandle.getUserId is hidden API
        return rootRepository.runRootCommand(
            "pm grant --user $userId ${BuildConfig.APPLICATION_ID} $PERMISSION_DUMP"
        )
    }

    override suspend fun grantDumpPermission(): Boolean {
        return withContext(Dispatchers.IO) {
            if(!hasDumpPermission()){
                val result = runGrantCommand()
                if(!result.isSuccess){
                    Log.w(
                        "PermissionRepository",
                        "DUMP grant failed: ${result.err.joinToString("\n")}"
                    )
                }
                hasDumpPermission()
            }else true
        }
    }

}
