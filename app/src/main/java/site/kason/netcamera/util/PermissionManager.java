package site.kason.netcamera.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class PermissionManager {

    private static int REQUEST_CODE = 0;

    private final String[] requiredPermissions;

    private final int requestCode;

    private final Activity activity;


    public PermissionManager(Activity activity, String... requiredPermissions) {
        this.requestCode = REQUEST_CODE++;
        this.requiredPermissions = requiredPermissions;
        this.activity = activity;
    }

    public boolean isGranted() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        for (String p : requiredPermissions) {
            int ret = activity.checkSelfPermission(p);
            if (ret != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void request() {
        activity.requestPermissions(requiredPermissions, requestCode);
    }

}
