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

import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;

/**
 * service for sending location beacons on a set interval to the server. This
 * can be disabled via the properties menu
 * 
 * @author Christopher Fagiani
 */
public class LocationService extends Service {
    private static Timer timer;
    private LocationManager locMgr;
    private Criteria locationCriteria;
    private static final long INITIAL_DELAY = 60000;
    private static final long INTERVAL = 1800000;
    private static boolean sendBeacon = true;
    private static final String BEACON_SERVICE_PATH = "/locationBeacon?action=beacon&phoneNumber=";
    private static final String IMEI = "&imei=";
    private static final String VER = "&ver=";
    private static final String LAT = "&lat=";
    private static final String LON = "&lon=";
    private static final String ACC = "&acc=";
    private static final String DEV_ID = "&devId=";
    private static final String OS_VERSION = "&osVersion=";
    private static final String TAG = "LocationService";
    private String version;
    private String deviceId;

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * life cycle method for the service. This is called by the system when the
     * service is started. It will schedule a timerTask that will periodically
     * check the current location and send it to the server
     */
    public int onStartCommand(final Intent intent, int flags, int startid) {
        // we only need to check this on command start since we'll explicitly
        // call endService if they change the preference to false after we're
        // already
        // started
        SurveyDbAdapter database = null;
        final String server = StatusUtil.getServerBase(this);
        try {
            database = new SurveyDbAdapter(this);

            database.open();
            String val = database
                    .getPreference(ConstantUtil.LOCATION_BEACON_SETTING_KEY);
            deviceId = database.getPreference(ConstantUtil.DEVICE_IDENT_KEY);
            if (val != null) {
                sendBeacon = Boolean.parseBoolean(val);
            }
            version = PlatformUtil.getVersionName(this);
        } finally {
            if (database != null) {
                database.close();
            }
        }
        // Safe to lazy initialize the static field, since this method
        // will always be called in the Main Thread
        if (timer == null && sendBeacon) {
            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    if (sendBeacon) {
                        String provider = locMgr.getBestProvider(
                                locationCriteria, true);
                        if (provider != null) {
                            sendLocation(server, locMgr.getLastKnownLocation(provider));
                        }
                    }
                }
            }, INITIAL_DELAY, INTERVAL);
        }
        return Service.START_STICKY;
    }

    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
    }

    /**
     * sends the location beacon to the server
     * 
     * @param loc
     */
    private void sendLocation(String serverBase, Location loc) {
        if (serverBase != null) {
            try {
                String phoneNumber = StatusUtil.getPhoneNumber(this);
                if (phoneNumber != null) {
                    String url = serverBase
                            + BEACON_SERVICE_PATH + URLEncoder.encode(phoneNumber, "UTF-8")
                            + IMEI + URLEncoder.encode(StatusUtil.getImei(this), "UTF-8");
                    if (loc != null) {
                        url += LAT + loc.getLatitude() + LON + loc.getLongitude()
                                + ACC + loc.getAccuracy();
                    }
                    url += VER + version;
                    url += OS_VERSION
                            + URLEncoder.encode("Android " + android.os.Build.VERSION.RELEASE);
                    if (deviceId != null) {
                        url += DEV_ID + URLEncoder.encode(deviceId, "UTF-8");
                    }
                    HttpUtil.httpGet(url);
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not send location beacon", e);
                PersistentUncaughtExceptionHandler.recordException(e);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
