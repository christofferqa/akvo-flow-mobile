/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.service;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;

/**
 * service that periodically checks for stack trace files on the file system
 * and, if found, uploads them to the server. After a file is uploaded, the
 * trace file is deleted from the file system.
 * 
 * @author Christopher Fagiani
 */
public class ExceptionReportingService extends Service {
    private static final String TAG = "EXCEPTION_REPORTING_SERVICE";
    private static final String EXCEPTION_SERVICE_PATH = "/remoteexception";
    private static final String ACTION_PARAM = "action";
    private static final String ACTION_VALUE = "saveTrace";
    private static final String PHONE_PARAM = "phoneNumber";
    private static final String IMEI_PARAM = "imei";
    private static final String VER_PARAM = "version";
    private static final String DEV_ID_PARAM = "deviceIdentifier";
    private static final String DATE_PARAM = "date";
    private static final String TRACE_PARAM = "trace";
    private static final long INITIAL_DELAY = 60000;
    private static final long INTERVAL = 300000;

    private static final ThreadLocal<DateFormat> DATE_FMT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    private static Timer timer;

    private String version;
    private String deviceId;
    private String phoneNumber;
    private String imei;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * life cycle method for the service. This is called by the system when the
     * service is started. It will schedule a timerTask that will periodically
     * check the file system for trace files and, if found, will send them to
     * the server via a REST call
     */
    public int onStartCommand(final Intent intent, int flags, int startid) {
        SurveyDbAdapter database = null;
        final String server = StatusUtil.getServerBase(this);

        try {
            database = new SurveyDbAdapter(this);
            database.open();
            deviceId = database.getPreference(ConstantUtil.DEVICE_IDENT_KEY);
            version = PlatformUtil.getVersionName(this);
            phoneNumber = StatusUtil.getPhoneNumber(this);
            imei = StatusUtil.getImei(this);
        } finally {
            if (database != null) {
                database.close();
            }
        }
        // Safe to lazy initialize the static field, since this method
        // will always be called in the Main Thread
        if (timer == null) {
            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    if (StatusUtil.hasDataConnection(ExceptionReportingService.this) &&
                            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        submitStackTraces(server);
                    }
                }
            }, INITIAL_DELAY, INTERVAL);
        }
        return Service.START_STICKY;
    }

    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
    }

    /**
     * Returns all stack trace files on the files system
     * 
     * @return
     */
    private String[] getTraceFiles(File dir) {
        FilenameFilter traceFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(ConstantUtil.STACKTRACE_SUFFIX);
            }
        };
        return dir.list(traceFilter);
    }

    /**
     * Look in the trace directory for any files. If any are found, upload to
     * the server and, on success, delete the file.
     */
    public void submitStackTraces(String server) {
        try {
            File dir = FileUtil.getFilesDir(FileType.STACKTRACE);
            String[] list = getTraceFiles(dir);
            if (list != null && list.length > 0) {
                for (int i = 0; i < list.length; i++) {
                    File f = new File(dir, list[i]);
                    String trace = FileUtil.readFileAsString(f);

                    Map<String, String> params = new HashMap<String, String>();
                    params.put(ACTION_PARAM, ACTION_VALUE);
                    params.put(PHONE_PARAM, phoneNumber);
                    params.put(IMEI_PARAM, imei);
                    params.put(VER_PARAM, version);
                    params.put(DATE_PARAM,
                            DATE_FMT.get().format(new Date(f.lastModified())));
                    params.put(DEV_ID_PARAM, deviceId);
                    params.put(TRACE_PARAM, trace);

                    String response = HttpUtil.httpPost(server + EXCEPTION_SERVICE_PATH, params);
                    if (response == null
                            || response.trim().length() == 0
                            || "ok".equalsIgnoreCase(response)) {
                        f.delete();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not send exception", e);
        }
    }

}
