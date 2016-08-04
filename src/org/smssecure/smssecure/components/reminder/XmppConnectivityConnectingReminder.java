package org.smssecure.smssecure.components.reminder;

import android.content.Context;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.service.XmppService;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.XmppUtil;

public class XmppConnectivityConnectingReminder extends Reminder {

  public XmppConnectivityConnectingReminder(final Context context) {
    super(context.getString(R.string.XmppService_connecting_to_xmpp_server),
          context.getString(R.string.XmppService_xmpp_features_in_silence_are_disabled),
          null);
  }

  public static boolean isEligible(Context context) {
    return SilencePreferences.isXmppRegistered(context) &&
          !XmppUtil.isXmppAvailable(context)            &&
          XmppService.getInstance().getConnectionStatus().equals(XmppService.CONNECTING);
  }

  public boolean isDismissable() {
    return false;
  }
}
