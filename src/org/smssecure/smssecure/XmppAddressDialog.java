package org.smssecure.smssecure;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.ServiceUtil;

import java.io.IOException;

/**
 * Activity for displaying XMPP address.
 */

public class XmppAddressDialog extends AlertDialog {
  private static final String TAG = XmppAddressDialog.class.getSimpleName();

  public static final String TEXT_PLAIN = "text/plain";

  private Context context;
  private String  silenceLink;

  public XmppAddressDialog(@NonNull Context context) {
    super(context);

    this.context = context;

    LayoutInflater layoutInflater = ((Activity)context).getLayoutInflater();
    View view = layoutInflater.inflate(R.layout.xmpp_address_dialog, null);

    setTitle(context.getString(R.string.XmppAddressDialog_share_xmpp_address));
    silenceLink = "https://s.silence.im/#" + SilencePreferences.getXmppJid(context);

    TextView xmppAddress = (TextView) view.findViewById(R.id.xmpp_address);
    xmppAddress.setText(silenceLink);
    xmppAddress.setOnClickListener(new CopyXmppAdressListener());

    setView(view);
    setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {}
    });
    setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.XmppAddressDialog_share), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        shareXmppAddress();
      }
    });
  }

  private void shareXmppAddress() {
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.XmppAddressDialog_here_is_my_silence_xmpp_address, silenceLink));
    sendIntent.setType(TEXT_PLAIN);
    context.startActivity(sendIntent);
  }

  @Override
  public void show() {
    super.show();
  }

  private class CopyXmppAdressListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        ServiceUtil.getClipboardManager(context).setText(silenceLink);
      } else {
        ClipData clipData = ClipData.newPlainText(context.getString(R.string.XmppAddressDialog_silence_xmpp_address), silenceLink);
        ServiceUtil.getClipboardManager(context).setPrimaryClip(clipData);
      }
      Toast.makeText(context.getApplicationContext(),
                     context.getString(R.string.XmppAddressDialog_xmpp_address_copied),
                     Toast.LENGTH_LONG).show();
    }
  }
}
