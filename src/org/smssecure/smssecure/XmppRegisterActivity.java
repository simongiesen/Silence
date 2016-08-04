package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.task.ProgressDialogAsyncTask;
import org.smssecure.smssecure.util.XmppUtil;
import org.smssecure.smssecure.XmppRegisterCustomActivity;

/**
 * Activity for registering an XMPP account on one of the trusted servers in
 * Silence or on a custom server.
 */
public class XmppRegisterActivity extends BaseActionBarActivity {
  private static final String TAG = XmppRegisterActivity.class.getSimpleName();

  private Context context;

  private static final int REGISTERING_CUSTOM_ACTIVITY_RESULT_CODE = 667;

  private DynamicTheme    dynamicTheme    = new DynamicTheme();
  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private Button trustedServerButton;
  private Button customServerButton;
  private Button cancelButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(savedInstanceState);

    this.context = XmppRegisterActivity.this;

    if (SilencePreferences.isXmppRegistered(context)) {
      Toast.makeText(context, R.string.XmppRegisterActivity__you_are_already_registered, Toast.LENGTH_LONG).show();
      finishActivity();
    }

    SilencePreferences.disableXmpp(context);
    setContentView(R.layout.register_xmpp_account);

    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  private void initializeResources() {
    this.trustedServerButton = (Button) findViewById(R.id.trusted_server_button);
    this.customServerButton  = (Button) findViewById(R.id.custom_server_button);
    this.cancelButton        = (Button) findViewById(R.id.cancel_button);

    this.trustedServerButton.setOnClickListener(new TrustedServerButtonClickListener());
    this.customServerButton.setOnClickListener(new CustomServerButtonClickListener());
    this.cancelButton.setOnClickListener(new CancelButtonClickListener());
  }

  private class CancelButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      SilencePreferences.disableXmpp(context);
      finishActivity();
    }
  }

  private class TrustedServerButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      new RegisterOnTrustedServerTask(context).execute();
    }
  }

  private class CustomServerButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      startActivityForResult(new Intent(context, XmppRegisterCustomActivity.class), REGISTERING_CUSTOM_ACTIVITY_RESULT_CODE);
    }
  }

  private class RegisterOnTrustedServerTask extends ProgressDialogAsyncTask<Void, Void, Void> {
    private final Context context;

    public RegisterOnTrustedServerTask(final Context context) {
      super(context,
            context.getString(R.string.register_xmpp_account_registration),
            context.getString(R.string.register_xmpp_account_creating_an_xmpp_account));
      this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        XmppUtil.tryToRegister(context);
        new Thread() {
          @Override
          public void run() {
            Looper.prepare();
            Toast.makeText(context.getApplicationContext(),
                           context.getString(R.string.XmppRegisterActivity__registered),
                           Toast.LENGTH_LONG).show();
            Looper.loop();
          }
        }.start();
        SilencePreferences.enableXmpp(context);
        XmppUtil.startService(context);
      } catch (Exception e) {
        Log.w(TAG, e);
        new Thread() {
          @Override
          public void run() {
            Looper.prepare();
            Toast.makeText(context,
                           context.getString(R.string.XmppRegisterActivity__cannot_create_an_xmpp_account_please_try_again_later),
                           Toast.LENGTH_LONG).show();
            Looper.loop();
          }
        }.start();
        SilencePreferences.disableXmpp(context);
      }
      finishActivity();
      return null;
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      SilencePreferences.disableXmpp(context);
      finishActivity();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  private void finishActivity() {
    setResult(Activity.RESULT_OK);
    finish();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REGISTERING_CUSTOM_ACTIVITY_RESULT_CODE &&
        resultCode  == Activity.RESULT_OK)
    {
      new Thread() {
        @Override
        public void run() {
          Looper.prepare();
          Toast.makeText(context.getApplicationContext(),
                         context.getString(R.string.XmppRegisterActivity__registered),
                         Toast.LENGTH_LONG).show();
          Looper.loop();
        }
      }.start();
      finishActivity();
    }
  }

}
