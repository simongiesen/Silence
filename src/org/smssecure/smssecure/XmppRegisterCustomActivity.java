package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.task.ProgressDialogAsyncTask;
import org.smssecure.smssecure.util.XmppUtil;
import org.smssecure.smssecure.util.XmppUtil.XmppServer;

/**
 * Activity for registering an XMPP account on a custom server.
 *
 * @author Bastien Le Querrec
 */
public class XmppRegisterCustomActivity extends BaseActionBarActivity {
  private static final String TAG = XmppRegisterCustomActivity.class.getSimpleName();

  public static final String REGISTERED_EXTRA = "REGISTERED";

  private Context context;

  private DynamicTheme    dynamicTheme    = new DynamicTheme();
  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private EditText hostnameText;
  private EditText portText;
  private Integer  port;

  private Button connectButton;
  private Button cancelButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.register_custom_xmpp_server);

    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  private void initializeResources() {
    this.connectButton = (Button) findViewById(R.id.connect_button);
    this.cancelButton  = (Button) findViewById(R.id.cancel_button);

    this.hostnameText = (EditText) findViewById(R.id.hostname);
    this.portText     = (EditText) findViewById(R.id.port);

    this.connectButton.setOnClickListener(new ConnectButtonClickListener());
    this.cancelButton.setOnClickListener(new CancelButtonClickListener());

    this.context = XmppRegisterCustomActivity.this;
  }

  private class CancelButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      finishActivity(false);
    }
  }

  private class ConnectButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      try {
        String portString = portText.getText().toString();
        if (portString == null || portString.equals("")) {
          port = null;
        } else {
          port = Integer.parseInt(portString);
          if (port > 65535 || port < 1) throw new NumberFormatException();
        }
        new RegisterCustomServerTask(hostnameText.getText().toString(), port).execute();
      } catch (NumberFormatException e) {
        Log.w(TAG, e);
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.register_xmpp_account_invalid_port), Snackbar.LENGTH_LONG).show();
      }
    }
  }

  private class RegisterCustomServerTask extends ProgressDialogAsyncTask<Void, Void, Void> {
    private final XmppServer xmppServer;

    public RegisterCustomServerTask(String hostname, Integer port) {
      super(context,
            context.getString(R.string.register_xmpp_account_registration),
            context.getString(R.string.register_xmpp_account_creating_an_xmpp_account));

      if (port == null) {
        this.xmppServer = new XmppServer(hostname);
      } else {
        this.xmppServer = new XmppServer(hostname, port);
      }
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        XmppUtil.register(context, xmppServer);
        SilencePreferences.enableXmpp(context);
        XmppUtil.startService(context);
        finishActivity(true);
      } catch (Exception e) {
        Log.w(TAG, e);
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.register_xmpp_account_cannot_register_on_this_server), Snackbar.LENGTH_LONG).show();
        SilencePreferences.disableXmpp(context);
      }
      return null;
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      finishActivity(false);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  private void finishActivity(boolean registered) {
    setResult(registered ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
    finish();
  }

}
