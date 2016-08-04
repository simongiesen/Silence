package org.smssecure.smssecure.components.reminder;

import android.content.Context;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.service.XmppService;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.XmppUtil;

public class XmppConnectivityDisconnectedReminder extends Reminder {

  public XmppConnectivityDisconnectedReminder(final Context context) {
    super(context.getString(R.string.XmppService_xmpp_connection_failed),
          context.getString(R.string.XmppService_xmpp_features_in_silence_are_disabled),
          null);
  }

  public static boolean isEligible(Context context) {
    return SilencePreferences.isXmppRegistered(context) &&
          !XmppUtil.isXmppAvailable(context)            &&
          XmppService.getInstance().getConnectionStatus().equals(XmppService.DISCONNECTED);
  }

  public boolean isDismissable() {
    return false;
  }
}
