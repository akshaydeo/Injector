package net.media.lib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import net.media.dynamiclib.IMethods;

/**
 * Loads an apk and init sdk
 */

public final class Injector {

  private static final String TAG = "## Injector ##";
  private static final String DEX_PATH = "http://172.16.61.23:8000/dynamiclib-release-unsigned.apk";
  private static String INTERNAL_DEX_PATH;
  private static String FILE_NAME = "/lib.apk";
  // declare the dialog as a member field of your activity
  private static ProgressDialog mProgressDialog;

  private interface TaskListener {
    void onComplete();
  }

  public static void load(final Context context) {
    Log.d(TAG, "load()");
    INTERNAL_DEX_PATH = context.getFilesDir() + FILE_NAME;
    // initiating the progress dialog
    mProgressDialog = new ProgressDialog(context);
    mProgressDialog.setMessage("A message");
    mProgressDialog.setIndeterminate(true);
    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    mProgressDialog.setCancelable(true);

    // execute this when the downloader must be fired
    final DownloadTask downloadTask = new DownloadTask(context, new TaskListener() {
      @Override public void onComplete() {
        loadSdk(context);
      }
    });
    downloadTask.execute(DEX_PATH);
    mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override public void onCancel(DialogInterface dialog) {
        downloadTask.cancel(true);
      }
    });
  }

  private static void loadSdk(Context context) {
    //// Downloading file
    PathClassLoader pathClassLoader =
        new PathClassLoader(INTERNAL_DEX_PATH, context.getClassLoader());

    try {
      Log.d(TAG, "loading sdk class");
      Class sdkClass = pathClassLoader.loadClass("net.media.dynamiclib.MethodsImpl");
      IMethods methods = (IMethods) sdkClass.newInstance();
      methods.log("testing this");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class DownloadTask extends AsyncTask<String, Integer, String> {

    private Context context;
    private TaskListener mListener;

    public DownloadTask(Context context, TaskListener listener) {
      this.context = context;
      mListener = listener;
    }

    @Override protected void onPreExecute() {
      super.onPreExecute();
      // take CPU lock to prevent CPU from going off if the user
      // presses the power button during download
      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      mProgressDialog.show();
    }

    @Override protected void onProgressUpdate(Integer... progress) {
      super.onProgressUpdate(progress);
      // if we get here, length is known, now set indeterminate to false
      mProgressDialog.setIndeterminate(false);
      mProgressDialog.setMax(100);
      mProgressDialog.setProgress(progress[0]);
    }

    @Override protected void onPostExecute(String result) {
      mProgressDialog.dismiss();
      if (result != null) {
        Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
      } else {
        Log.d(TAG, "downloaded");
        for (File file : context.getFilesDir().listFiles()) {
          Log.d(TAG, file.getAbsolutePath());
        }
        Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
        mListener.onComplete();
      }
    }

    @Override protected String doInBackground(String... sUrl) {
      InputStream input = null;
      OutputStream output = null;
      HttpURLConnection connection = null;
      try {
        URL url = new URL(sUrl[0]);
        connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        // expect HTTP 200 OK, so we don't mistakenly save error report
        // instead of the file
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
          return "Server returned HTTP "
              + connection.getResponseCode()
              + " "
              + connection.getResponseMessage();
        }

        // this will be useful to display download percentage
        // might be -1: server did not report the length
        int fileLength = connection.getContentLength();

        // download the file
        input = connection.getInputStream();
        output = new FileOutputStream(context.getFilesDir() + FILE_NAME);
        Log.d(TAG, "File output stream is => " + context.getFilesDir() + FILE_NAME);
        byte data[] = new byte[4096];
        long total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
          // allow canceling with back button
          if (isCancelled()) {
            input.close();
            return null;
          }
          total += count;
          // publishing the progress....
          if (fileLength > 0) // only if total length is known
          {
            publishProgress((int) (total * 100 / fileLength));
          }
          output.write(data, 0, count);
        }
      } catch (Exception e) {
        Log.e(TAG, "While downloaing file", e);
        return e.toString();
      } finally {
        try {
          if (output != null) output.close();
          if (input != null) input.close();
        } catch (IOException ignored) {
          Log.e(TAG, "While downloaing file", ignored);
        }

        if (connection != null) connection.disconnect();
      }
      return null;
    }
  }
}
