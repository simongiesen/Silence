package org.smssecure.smssecure.sms;

import org.smssecure.smssecure.recipients.Recipients;

public class OutgoingXmppExchangeMessage extends OutgoingTextMessage {

  public OutgoingXmppExchangeMessage(OutgoingTextMessage base) {
    this(base, base.getMessageBody());
  }

  public OutgoingXmppExchangeMessage(OutgoingTextMessage message, String body) {
    super(message, body);
  }

  public OutgoingXmppExchangeMessage(Recipients recipients, String message, int subscriptionId) {
    super(recipients, message, subscriptionId);
  }

  @Override
  public boolean isXmppExchange() {
    return true;
  }

  @Override
  public OutgoingTextMessage withBody(String body) {
    return new OutgoingXmppExchangeMessage(this, body);
  }
}
