package frb.astrostar.server.shell

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import frb.astrostar.api.AstroStar
import frb.astrostar.server.ServerConstants
import frb.astrostar.shared.AstroStarApiConstant

object ShellBinderRequestHandler {

    fun handleRequest(context: Context, intent: Intent): Boolean {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXRUNTIME) {
            return false
        }

        val binder = intent.getBundleExtra("data")?.getBinder("binder") ?: return false
        val astrostarBinder = AstroStar.getBinder()
//        if (astrostarBinder == null) {
//            LOGGER.w("Binder not received or AxManager service not running")
//        }

        val data = Parcel.obtain()
        return try {
            data.writeStrongBinder(astrostarBinder)
            data.writeLong(AstroStarApiConstant.server.VERSION_CODE)
            data.writeString(context.applicationInfo.nativeLibraryDir)
            binder.transact(1, data, null, IBinder.FLAG_ONEWAY)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        } finally {
            data.recycle()
        }
    }
}
