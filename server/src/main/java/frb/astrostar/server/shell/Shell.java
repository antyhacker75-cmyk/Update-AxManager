package frb.astrostar.server.shell;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;

import frb.astrostar.api.AstroStar;
import frb.astrostar.shared.AstroStarApiConstant;
import rikka.rish.Rish;
import rikka.rish.RishConfig;

public class Shell extends Rish {

    public static void main(String[] args, String packageName, IBinder binder, Handler handler) {
        RishConfig.init(binder, AstroStarApiConstant.server.BINDER_DESCRIPTOR, 24 * 60 * 60 * 1000);
        AstroStar.onBinderReceived(binder, packageName);
        AstroStar.addBinderReceivedListenerSticky(() -> {
            handler.post(() -> new Shell().start(args));
        });
    }

    @Override
    public void requestPermission(Runnable onGrantedRunnable) {
        if (AstroStar.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onGrantedRunnable.run();
        } else if (AstroStar.shouldShowRequestPermissionRationale()) {
            System.err.println("Permission denied");
            System.err.flush();
            System.exit(1);
        } else {
            AstroStar.addRequestPermissionResultListener(new AstroStar.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    AstroStar.removeRequestPermissionResultListener(this);

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        onGrantedRunnable.run();
                    } else {
                        System.err.println("Permission denied");
                        System.err.flush();
                        System.exit(1);
                    }
                }
            });
            AstroStar.requestPermission(0);
        }
    }
}
