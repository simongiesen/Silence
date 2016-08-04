package org.smssecure.smssecure.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.XmppUtil;

public class XmppServiceBoot extends BroadcastReceiver {
  private static final String TAG = XmppServiceBoot.class.getSimpleName();

  private static final String CONNECTIVITY_CHANGE_EVENT = "android.net.conn.CONNECTIVITY_CHANGE";
  private static final String WIFI_STATE_CHANGED = "android.net.wifi.WIFI_STATE_CHANGED";

  private Context context;

  @Override
  public void onReceive(final Context context, Intent intent) {
    Log.w(TAG, "onReceive()");
    this.context = context;

    switch (intent.getAction()) {
      case CONNECTIVITY_CHANGE_EVENT:
        restartXmppService();
        break;
      case WIFI_STATE_CHANGED:
        restartXmppService();
        break;
      default:
        throw new AssertionError("Unknown event passed to XmppServiceBoot");
    }
  }

  private void restartXmppService() {
    Log.w(TAG, "restartXmppService()");
    XmppService xmppService = XmppService.getInstance();
    if (xmppService != null) xmppService.reconnect();
  }

}
