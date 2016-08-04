package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.ServiceUtil;

import java.io.IOException;

/**
 * Activity for entering XMPP address manually.
 */

public class XmppEnterAddressDialog extends AlertDialog {
  private static final String TAG = XmppEnterAddressDialog.class.getSimpleName();

  private Recipient recipient;
  private View      view;
  private Context   context;

  public XmppEnterAddressDialog(@NonNull Context context, @NonNull Recipient recipient) {
    super(context);
    this.context   = context;
    this.recipient = recipient;

    LayoutInflater layoutInflater = ((Activity)context).getLayoutInflater();

    view = layoutInflater.inflate(R.layout.xmpp_enter_address_dialog, null);

    TextView xmppText = (TextView) view.findViewById(R.id.xmpp_text);

    xmppText.setText(context.getString(R.string.xmpp_enter_address_dialog__use_this_form_to_enter_the_xmpp_address_of, recipient.getName()));

    setView(view);
    setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (OnClickListener) null);
    setButton(AlertDialog.BUTTON_POSITIVE, context.getString(android.R.string.ok), new EnterXmppAdressListener());
  }

  @Override
  public void show() {
    super.show();
  }

  private class EnterXmppAdressListener implements AlertDialog.OnClickListener {
    @Override
    public void onClick(DialogInterface dialog, int which) {
      TextView xmppAddress = (TextView) view.findViewById(R.id.xmpp_address);
      if (xmppAddress != null) recipient.setXmppJid(xmppAddress.getText() == null ? null : xmppAddress.getText().toString());
      Toast.makeText(context,
                     context.getString(R.string.XmppEnterAddressDialog_done),
                     Toast.LENGTH_LONG).show();
    }
  }
}
