package site.kason.netcamera.util;

import android.hardware.Camera;

import java.util.List;

public class CameraUtil {

    public static boolean setFocusMode(Camera.Parameters parameters, String mode) {
        List<String> modes = parameters.getSupportedFocusModes();
        if (modes.contains(mode)) {
            parameters.setFocusMode(mode);
            return true;
        }
        return false;
    }

    public static boolean setPreferredPreviewFormat(Camera.Parameters parameters, int... preferredFormat) {
        List<Integer> fmts = parameters.getSupportedPreviewFormats();
        for (int pf : preferredFormat) {
            if (fmts.contains(pf)) {
                parameters.setPreviewFormat(pf);
                return true;
            }
        }
        return false;
    }

    public static Camera.Size setNearSize(Camera.Parameters parameters, int preferWith, int preferHeight) {
        Camera.Size size = selectNearSize(parameters, preferWith, preferHeight);
        parameters.setPreviewSize(size.width, size.height);
        return size;
    }

    public static Camera.Size selectNearSize(Camera.Parameters parameters, int preferWith, int preferHeight) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            if (size.width <= preferWith && size.height <= preferHeight) {
                return size;
            }
        }
        return sizes.get(sizes.size() - 1);
    }

}
