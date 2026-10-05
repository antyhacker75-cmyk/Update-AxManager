package frb.astrostar.manager.ui.viewmodel

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import frb.astrostar.adb.AdbPairingService
import frb.astrostar.adb.util.AdbEnvironment
import frb.astrostar.api.AstroStar
import frb.astrostar.api.AstroStarCommandSession
import frb.astrostar.api.AstroStarInfo
import frb.astrostar.api.core.AstroStarSettings
import frb.astrostar.api.core.Starter
import frb.astrostar.manager.adb.AdbStarter
import frb.astrostar.manager.adb.AdbStarter.stopTcp
import frb.astrostar.manager.adb.AdbStateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ActivateViewModel : ViewModel() {

    companion object {
        const val TAG = "AdbViewModel"
        const val ACTIVATE_FAILED = -1
        const val ACTIVATE_PROCESS = 0
        const val ACTIVATE_SUCCESS = 1

    }

    var activateStatus by mutableStateOf<ActivateStatus>(run {
        if (AstroStar.pingBinder() && AstroStar.getAstroStarInfo().isNeedUpdate()) {
            ActivateStatus.Updating(AstroStar.getAstroStarInfo())
        }
        ActivateStatus.Disable
    })
        private set

    var astrostarInfo by mutableStateOf(AstroStarInfo())
        private set

    var isShizukuActive by mutableStateOf(
        AstroStar.getShizukuService() != null
    )
        private set

    fun setShizukuIntercept(enable: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            isShizukuActive = enable
            AstroStar.enableShizukuService(enable)
        }
    }

    fun checkShizukuIntercept() {
        viewModelScope.launch(Dispatchers.Main) {
            isShizukuActive = AstroStar.pingBinder() && AstroStar.getShizukuService() != null
        }
    }

    var isNotificationEnabled by mutableStateOf(false)
        private set

    var devSettings by mutableStateOf(false)
        private set

    fun setLaunchDevSettings(launch: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            devSettings = launch
        }
    }

    var tryActivate by mutableStateOf(false)
        private set

    fun setTryToActivate(activate: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            tryActivate = activate
        }
    }

    fun resetStatus() {
        activateStatus = ActivateStatus.Disable
    }

    suspend fun awaitRunning(timeout: Long = 10000) {
        if (activateStatus is ActivateStatus.Running) return
        withTimeoutOrNull(timeout) {
            snapshotFlow { activateStatus }.first { it is ActivateStatus.Running }
        }
    }


    sealed class ActivateStatus {
        object Disable : ActivateStatus()
        object NeedExtraStep : ActivateStatus()
        class Updating(val astrostarInfo: AstroStarInfo) : ActivateStatus()
        class Running(val astrostarInfo: AstroStarInfo) : ActivateStatus()
    }

    fun astrostarObserve(): Flow<ActivateStatus> = callbackFlow {
        if (AstroStar.pingBinder()) {
            Log.i("AxManagerBinder", "binderHasReceived")
            val astrostarInfo = AstroStar.getAstroStarInfo()
            when {
                astrostarInfo.isNeedUpdate() -> {
                    trySend(ActivateStatus.Updating(astrostarInfo))
                    setTryToActivate(true)
                    AstroStar.newProcess(
                        AstroStarCommandSession.getQuickCmd(
                            Starter.internalCommand,
                            true,
                            false
                        ),
                        null,
                        null
                    )
                }

                astrostarInfo.isRunning() -> {
                    trySend(ActivateStatus.Running(astrostarInfo))
                }

                astrostarInfo.isNeedExtraStep() -> {
                    trySend(ActivateStatus.NeedExtraStep)
                }
            }
        }
        val receivedListener = AstroStar.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onBinderReceived")
            val astrostarInfo = AstroStar.getAstroStarInfo()
            when {
                astrostarInfo.isRunning() -> {
                    trySend(ActivateStatus.Running(astrostarInfo))
                }

                astrostarInfo.isNeedExtraStep() -> {
                    trySend(ActivateStatus.NeedExtraStep)
                }
            }
        }
        val deadListener = AstroStar.OnBinderDeadListener {
            Log.i("AxManagerBinder", "onBinderDead")
            trySend(ActivateStatus.Disable)
        }
        AstroStar.addBinderReceivedListener(receivedListener)
        AstroStar.addBinderDeadListener(deadListener)
        awaitClose {
            AstroStar.removeBinderReceivedListener(receivedListener)
            AstroStar.removeBinderDeadListener(deadListener)
        }
    }

    init {
        viewModelScope.launch {
            astrostarObserve().collect { status ->
                val isStillUpdating =
                    status is ActivateStatus.Disable && activateStatus is ActivateStatus.Updating
                astrostarInfo = when (status) {
                    is ActivateStatus.Running -> {
                        checkShizukuIntercept()
                        status.astrostarInfo
                    }

                    is ActivateStatus.Updating -> {
                        status.astrostarInfo
                    }

                    else -> {
                        if (isStillUpdating) {
                            (activateStatus as ActivateStatus.Updating).astrostarInfo
                        } else {
                            AstroStarInfo()
                        }
                    }
                }
                if (isStillUpdating) return@collect
                Log.i("AxManagerBinder", "status: $status")
                activateStatus = status
                setTryToActivate(false)
            }
        }
    }

    suspend fun startRoot(): Int = withContext(Dispatchers.IO) {
        runCatching {
            if (tryActivate) return@withContext ACTIVATE_PROCESS
            setTryToActivate(true)

            if (!Shell.getShell().isRoot) {
                Shell.getCachedShell()?.close()
                return@withContext ACTIVATE_FAILED
            }

            val result = Shell.cmd(Starter.internalCommand).exec()
            if (result.isSuccess) {
                AstroStarSettings.setLastLaunchMode(AstroStarSettings.LaunchMethod.ROOT)
                ACTIVATE_SUCCESS
            } else {
                ACTIVATE_FAILED
            }
        }.getOrElse {
            it.printStackTrace()
            ACTIVATE_FAILED
        }.also {
            Shell.getCachedShell()?.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun startAdbWireless(
        context: Context
    ): AdbStateInfo = withContext(Dispatchers.IO) {
        if (AdbEnvironment.isWifiRequired() && !isWifiEnabled(context)) {
            requestEnableWifi(context)
            return@withContext AdbStateInfo.Failed("WiFi is required")
        }
        if (tryActivate) return@withContext AdbStateInfo.Process("Trying to activate")
        setTryToActivate(true)
        resetStatus()

        val resultChannel = kotlinx.coroutines.channels.Channel<AdbStateInfo>(1)
        val job = launch {
            AdbStarter.startAdbWireless(context) {
                resultChannel.trySend(it)
            }
        }

        val result = withTimeoutOrNull(15000) {
            resultChannel.receive()
        } ?: AdbStateInfo.Failed("Timeout waiting for connection")

        job.cancel()
        result
    }

    suspend fun startAdbTcp(
        context: Context
    ): AdbStateInfo = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext AdbStateInfo.Process("Trying to activate")
        setTryToActivate(true)
        resetStatus()

        val tcpPort = AdbEnvironment.getAdbTcpPort()

        val resultChannel = kotlinx.coroutines.channels.Channel<AdbStateInfo>(1)
        AdbStarter.startAdbClient(context, tcpPort) {
            resultChannel.trySend(it)
        }
        resultChannel.receive()
    }

    suspend fun stopAdbTcp(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext result(AdbStateInfo.Process("Trying to activate"))
        setTryToActivate(true)

        val tcpPort = AdbEnvironment.getAdbTcpPort()
        if (tcpPort > 0 && !AstroStarSettings.getTcpMode()) {
            stopTcp(context, tcpPort)
        }
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    fun requestEnableWifi(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        context.startActivity(intent)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun startPairingService(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isNotificationEnabled) return@launch
            setLaunchDevSettings(true)

            val intent = AdbPairingService.startIntent(context)
            try {
                context.startForegroundService(intent)
            } catch (e: Throwable) {
                Log.e("AxManager", "startForegroundService", e)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e is ForegroundServiceStartNotAllowedException
                ) {
                    val mode = context.getSystemService(AppOpsManager::class.java)
                        .noteOpNoThrow(
                            "android:start_foreground",
                            android.os.Process.myUid(),
                            context.packageName,
                            null,
                            null
                        )
                    if (mode == AppOpsManager.MODE_ERRORED) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "OP_START_FOREGROUND is denied. What are you doing?",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    context.startService(intent)
                }
            }
        }
    }


    /**
     * Cek notifikasi aktif atau tidak
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkNotificationEnabled(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }
}