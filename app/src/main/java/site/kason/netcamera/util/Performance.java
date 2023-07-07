package site.kason.netcamera.util;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class Performance {

    static class BeginInfo {
        long beginTime;
        long[] data;
        int groupCount;
        int dataCount;
    }

    private final static String TAG = Performance.class.getName();

    private int m_DefaultGroupCount = 100;

    private Map<String, BeginInfo> m_StartMap = new HashMap<>();


    public void begin(String name) {
        begin(name, m_DefaultGroupCount, System.currentTimeMillis());
    }

    public void begin(String name, long beginTickCount) {
        begin(name, m_DefaultGroupCount, beginTickCount);
    }

    public void begin(String name, int groupCount, long beginTickCount) {
        BeginInfo it = m_StartMap.get(name);
        if (it == null) {
            BeginInfo bi = new BeginInfo();
            bi.beginTime = beginTickCount;
            bi.groupCount = groupCount;
            bi.dataCount = 0;
            bi.data = new long[groupCount];
            m_StartMap.put(name, bi);
        } else {
            it.beginTime = beginTickCount;
        }
    }

    public void end(String name) {
        long endTs = System.currentTimeMillis();
        BeginInfo bi = m_StartMap.get(name);
        if (bi == null) {
            printf("Performance:%s not started\n", name);
            return;
        }
        if (bi.beginTime <= 0) {
            printf("Performance: %s not started\n", name);
            return;
        }
        long cost = endTs - bi.beginTime;
        if (bi.dataCount >= bi.groupCount - 1) {
            long total = 0;
            for (int i = 0; i < bi.dataCount; i++) {
                total += bi.data[i];
            }
            total += cost;
            long result = total / (bi.dataCount + 1);
            printf("Performance: %s %d ms\n", name, result);
            m_StartMap.remove(name);
            return;
        }
        bi.data[bi.dataCount] = cost;
        bi.dataCount++;
        bi.beginTime = 0;
    }

    private static void printf(String format, Object... args) {
        Log.i(TAG, String.format(format, args));
    }

}
