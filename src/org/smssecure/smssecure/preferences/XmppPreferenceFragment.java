package org.smssecure.smssecure.preferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.smssecure.smssecure.ApplicationPreferencesActivity;
import org.smssecure.smssecure.components.SwitchPreferenceCompat;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.RecipientPreferenceDatabase.RecipientsPreferences;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.service.KeyCachingService;
import org.smssecure.smssecure.service.XmppService;
import org.smssecure.smssecure.sms.MessageSender;
import org.smssecure.smssecure.sms.OutgoingTextMessage;
import org.smssecure.smssecure.sms.OutgoingXmppExchangeMessage;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.task.ProgressDialogAsyncTask;
import org.smssecure.smssecure.util.XmppUtil;
import org.smssecure.smssecure.XmppRegisterActivity;

import java.util.LinkedList;
import java.util.List;

public class XmppPreferenceFragment extends PreferenceFragment {

  private static final String TAG = XmppPreferenceFragment.class.getSimpleName();

  private static final int REGISTERING_ACTIVITY_RESULT_CODE = 666;

  private BroadcastReceiver xmppUpdateReceiver;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);
    addPreferencesFromResource(R.xml.preferences_xmpp);

    setXmppUiSettings();

    findPreference(SilencePreferences.XMPP_ENABLED_PREF)
        .setOnPreferenceChangeListener(new RegisterXmppListener());

    findPreference(SilencePreferences.XMPP_NOTIFY_CONTACTS)
        .setOnPreferenceClickListener(new XmppNotifyContactsListener());

    xmppUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        setXmppUiSettings();
      }
    };

    getActivity().registerReceiver(xmppUpdateReceiver, new IntentFilter(XmppService.XMPP_CONNECTIVITY_EVENT));

  }

  private static int getUiString(Context context) {
    if (SilencePreferences.isXmppRegistered(context)) {
      return XmppService.getInstance().isConnected() ?
                         R.string.preferences__xmpp_status_registered_connected :
                         R.string.preferences__xmpp_status_registered_disconnected;
    } else {
      return R.string.preferences__xmpp_status_unregistered;
    }
  }

  private void setXmppUiSettings() {
    Context context = getActivity();

    if (!SilencePreferences.isXmppRegistered(context)) {
      ((SwitchPreferenceCompat) findPreference(SilencePreferences.XMPP_ENABLED_PREF)).setChecked(false);
    }

    findPreference(SilencePreferences.XMPP_STATUS).setSummary(XmppPreferenceFragment.getUiString(context));
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity)getActivity()).getSupportActionBar().setTitle(R.string.preferences__xmpp);
  }

  private class RegisterXmppListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      final Context context = (Context) getActivity();

      if (!SilencePreferences.isXmppRegistered(context)) {
        startActivityForResult(new Intent(getActivity(), XmppRegisterActivity.class), REGISTERING_ACTIVITY_RESULT_CODE);
      } else {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.preferences__xmpp_unregistering_xmpp);
        builder.setMessage(R.string.preferences__xmpp_unregistering_from_xmpp_will_delete_the_account_on_the_server);
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            ((SwitchPreferenceCompat) findPreference(SilencePreferences.XMPP_ENABLED_PREF)).setChecked(true);
          }
        });
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            XmppUtil.sendNullXmppMessage(context);
            XmppService.getInstance().deleteAccount();
            findPreference(SilencePreferences.XMPP_STATUS).setSummary(R.string.preferences__xmpp_status_unregistered);
            new Thread() {
              @Override
              public void run() {
                Looper.prepare();
                Toast.makeText(context.getApplicationContext(),
                               context.getString(R.string.XmppRegisterActivity__unregistered),
                               Toast.LENGTH_LONG).show();
                Looper.loop();
              }
            }.start();
          }
        });
        builder.show();
      }
      return true;
    }
  }

  public static CharSequence getSummary(Context context) {
    return context.getString(getUiString(context));
  }

  private class XmppNotifyContactsTask extends ProgressDialogAsyncTask<Void, Void, Void> {
    private final Context context;

    public XmppNotifyContactsTask(final Context context) {
      super(context,
            context.getString(R.string.preferences__xmpp_notifying),
            context.getString(R.string.preferences__xmpp_sending_messages_to_contacts));
      this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
      sendXmppExchangeMessages(context);
      return null;
    }
  }

  private class XmppNotifyContactsListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      new XmppNotifyContactsTask(getActivity()).execute();

      return true;
    }
  }

  private void sendXmppExchangeMessages(final Context context) {
    final MasterSecret masterSecret = KeyCachingService.getMasterSecret(context);

    Recipients secureRecipients = RecipientFactory.getSecureRecipients(context, masterSecret, false);

    Log.w(TAG, "Sending SMS message to: " + secureRecipients.toShortString());

    for (Recipient recipient : secureRecipients) {
      final int subscriptionId = -1; // Is there a way to get the selected SIM in conversation?

      Recipients recipients = RecipientFactory.getRecipientsFor(context, recipient, false);

      OutgoingXmppExchangeMessage xmppExchangeMessage =
          new OutgoingXmppExchangeMessage(new OutgoingTextMessage(recipients,
                                                                  SilencePreferences.getXmppUsername(context) + "@" + SilencePreferences.getXmppHostname(context),
                                                                  subscriptionId));

      MessageSender.send(context, masterSecret, xmppExchangeMessage, -1, false);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REGISTERING_ACTIVITY_RESULT_CODE) {
      setXmppUiSettings();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (xmppUpdateReceiver != null) getActivity().unregisterReceiver(xmppUpdateReceiver);
  }
}
