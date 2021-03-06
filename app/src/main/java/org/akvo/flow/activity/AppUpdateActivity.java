/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;
import org.apache.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class AppUpdateActivity extends Activity implements View.OnClickListener {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_VERSION = "version";

    private static final String TAG = AppUpdateActivity.class.getSimpleName();
    private static final int IO_BUFFER_SIZE = 8192;
    private static final int MAX_PROGRESS = 100;

    private Button mButton;
    private ProgressBar mProgress;
    private UpdateAsyncTask mTask;

    String mUrl, mVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_update_activity);

        mUrl = getIntent().getStringExtra(EXTRA_URL);
        mVersion = getIntent().getStringExtra(EXTRA_VERSION);

        mProgress = (ProgressBar)findViewById(R.id.progress);
        mButton = (Button)findViewById(R.id.cancel_btn);

        mProgress.setMax(MAX_PROGRESS);// Values will be in percentage
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!isRunning()) {
            mButton.setText(R.string.cancelbutton);
            mTask = new UpdateAsyncTask();
            mTask.execute();
        } else {
            // Stop the update process
            mButton.setText(R.string.download_and_install);
            mTask.cancel(true);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRunning()) {
            mTask.cancel(true);
        }
        super.onBackPressed();
    }

    private boolean isRunning() {
        return mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING;
    }

    class UpdateAsyncTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            // Create parent directories, and delete files, if necessary
            String filename = createFile(mUrl, mVersion).getAbsolutePath();

            if (downloadApk(mUrl, filename) && !isCancelled()) {
                return filename;
            }
            // Clean up sd-card to ensure no corrupted file is leaked.
            cleanupDownloads(mVersion);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            int bytesWritten = progress[0];
            int totalBytes = progress[1];
            int percentComplete = 0;

            if (bytesWritten > 0 && totalBytes > 0) {
                percentComplete = (int) ((bytesWritten) / ((float) totalBytes) * 100);
            }
            if (percentComplete > MAX_PROGRESS) {
                percentComplete = MAX_PROGRESS;
            }

            Log.d(TAG, "onProgressUpdate() - APK update: " + percentComplete + "%");
            mProgress.setProgress(percentComplete);
        }

        @Override
        protected void onPostExecute(String filename) {
            if (TextUtils.isEmpty(filename)) {
                Toast.makeText(AppUpdateActivity.this, R.string.apk_upgrade_error, Toast.LENGTH_SHORT).show();
                mButton.setText(R.string.retry);
                return;
            }

            PlatformUtil.installAppUpdate(AppUpdateActivity.this, filename);
            finish();
        }

        @Override
        protected void onCancelled () {
            Log.d(TAG, "onCancelled() - APK update task cancelled");
            mProgress.setProgress(0);
            cleanupDownloads(mVersion);
        }

        private void cleanupDownloads(String version) {
            File directory = new File(FileUtil.getFilesDir(FileType.APK), version);
            FileUtil.deleteFilesInDirectory(directory, true);
        }

        /**
         * Wipe any existing apk file, and create a new File for the new one, according to the
         * given version
         * @param location
         * @param version
         * @return
         */
        private File createFile(String location, String version) {
            cleanupDownloads(version);

            String fileName = location.substring(location.lastIndexOf('/') + 1);
            File directory = new File(FileUtil.getFilesDir(FileType.APK), version);
            if (!directory.exists()) {
                directory.mkdir();
            }

            return new File(directory, fileName);
        }

        /**
         * Downloads the apk file and stores it on the file system
         * After the download, a new notification will be displayed, requesting
         * the user to 'click to installAppUpdate'
         *
         * @param remoteFile
         * @param surveyId
         */
        private boolean downloadApk(String location, String localPath) {
            Log.i(TAG, "App Update: Downloading new version " + mVersion + " from " + mUrl);
            if (!StatusUtil.hasDataConnection(AppUpdateActivity.this)) {
                Log.e(TAG, "No internet connection. Can't perform the requested operation");
                return false;
            }

            boolean ok = false;
            InputStream in = null;
            OutputStream out = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(location);
                conn = (HttpURLConnection) url.openConnection();

                in = new BufferedInputStream(conn.getInputStream());
                out = new BufferedOutputStream(new FileOutputStream(localPath));

                int bytesWritten = 0;
                byte[] b = new byte[IO_BUFFER_SIZE];

                final int fileSize = conn.getContentLength();
                Log.d(TAG, "APK size: " + fileSize);

                int read;
                while ((read = in.read(b)) != -1) {
                    if (isCancelled()) {
                        return false; // No need to continue the download
                    }
                    out.write(b, 0, read);
                    bytesWritten += read;
                    publishProgress(bytesWritten, fileSize);
                }
                out.flush();

                final int status = conn.getResponseCode();

                if (status == HttpStatus.SC_OK) {
                    Map<String, List<String>> headers = conn.getHeaderFields();
                    String etag = headers != null ? getHeader(headers, "ETag") : null;
                    etag = etag != null ? etag.replaceAll("\"", "") : null;// Remove quotes
                    final String checksum = FileUtil.hexMd5(new File(localPath));

                    if (etag != null && etag.equals(checksum)) {
                        ok = true;
                    } else {
                        Log.e(TAG, "ETag comparison failed. Remote: " + etag + " Local: " + checksum);
                        ok = false;
                    }
                } else {
                    Log.e(TAG, "Wrong status code: " + status);
                    ok = false;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    out.close();
                } catch (Exception ignored) {}
                try {
                    in.close();
                } catch (Exception ignored) {}
            }

            return ok;
        }

        /**
         * Helper function to get a particular header field from the header map
         * @param headers
         * @param key
         * @return header value, if found, false otherwise.
         */
        private String getHeader(Map<String, List<String>> headers, String key) {
            List<String> values = headers.get(key);
            if (values != null && values.size() > 0) {
                return values.get(0);
            }
            return null;
        }
    }
}
