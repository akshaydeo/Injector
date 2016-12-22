package net.media.dynamiclib;

import android.util.Log;

/**
 * Author : Akshay Deo
 * Date   : 21/12/16 3:19 PM
 * Email  : akshay.d@media.net
 */

public class MethodsImpl implements IMethods {
  private static final String TAG = "##MethodsImpl##";

  @Override public String getVersion() {
    return "0.1.1";
  }

  @Override public void log(String message) {
    Log.d(TAG, "Printing message");
  }
}
