package org.smssecure.smssecure.jobs;

import android.content.Context;
import android.telephony.SmsMessage;
import android.util.Log;
import android.util.Pair;

import org.smssecure.smssecure.ApplicationContext;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.MasterSecretUtil;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.EncryptingSmsDatabase;
import org.smssecure.smssecure.notifications.MessageNotifier;
import org.smssecure.smssecure.protocol.WirePrefix;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.service.KeyCachingService;
import org.smssecure.smssecure.sms.IncomingTextMessage;
import org.smssecure.smssecure.sms.MultipartSmsMessageHandler;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.libaxolotl.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;

public class SmsReceiveJob extends ContextJob {

  private static final long serialVersionUID = 1L;

  private static final String TAG = SmsReceiveJob.class.getSimpleName();

  private static MultipartSmsMessageHandler multipartMessageHandler = new MultipartSmsMessageHandler();

  private final Object[] pdus;
  private final int      subscriptionId;
  private final String   body;
  private final String   senderNumber;

  public SmsReceiveJob(Context context, Object[] pdus, int subscriptionId) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withWakeLock(true)
                                .create());

    this.pdus           = pdus;
    this.subscriptionId = subscriptionId;
    this.body           = null;
    this.senderNumber   = null;
  }

  public SmsReceiveJob(Context context, String body, String senderNumber, int subscriptionId) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withWakeLock(true)
                                .create());
    this.pdus           = null;
    this.subscriptionId = subscriptionId;
    this.body           = body;
    this.senderNumber   = senderNumber;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() {
    Optional<IncomingTextMessage> message;

    if (pdus != null) {
      message = assembleMessageFragments(pdus, subscriptionId);
    } else {
      IncomingTextMessage incomingTextMessage = new IncomingTextMessage(senderNumber, 1, System.currentTimeMillis(), body);
      if (WirePrefix.isPrefixedMessage(incomingTextMessage.getMessageBody())) {
        message = Optional.fromNullable(multipartMessageHandler.processPotentialMultipartMessage(incomingTextMessage));
      } else {
        message = Optional.of(incomingTextMessage);
      }
    }

    if (message.isPresent() && !isBlocked(message.get())) {
      Pair<Long, Long> messageAndThreadId = storeMessage(message.get());
      MessageNotifier.updateNotification(context, KeyCachingService.getMasterSecret(context), messageAndThreadId.second);
    } else if (message.isPresent()) {
      Log.w(TAG, "*** Received blocked SMS, ignoring...");
    }
  }

  @Override
  public void onCanceled() {

  }

  @Override
  public boolean onShouldRetry(Exception exception) {
    return false;
  }

  private boolean isBlocked(IncomingTextMessage message) {
    if (message.getSender() != null) {
      Recipients recipients = RecipientFactory.getRecipientsFromString(context, message.getSender(), false);
      return recipients.isBlocked();
    }

    return false;
  }

  private Pair<Long, Long> storeMessage(IncomingTextMessage message) {
    EncryptingSmsDatabase database     = DatabaseFactory.getEncryptingSmsDatabase(context);
    MasterSecret          masterSecret = KeyCachingService.getMasterSecret(context);

    Pair<Long, Long> messageAndThreadId;

    if (message.isSecureMessage()) {
      messageAndThreadId = database.insertMessageInbox((MasterSecret)null, message);
    } else if (masterSecret == null) {
      messageAndThreadId = database.insertMessageInbox(MasterSecretUtil.getAsymmetricMasterSecret(context, null), message);
    } else {
      messageAndThreadId = database.insertMessageInbox(masterSecret, message);
    }

    if (masterSecret == null || message.isSecureMessage() || message.isKeyExchange() || message.isEndSession() || message.isXmppExchange()) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new SmsDecryptJob(context, messageAndThreadId.first));
    }

    return messageAndThreadId;
  }

  private Optional<IncomingTextMessage> assembleMessageFragments(Object[] pdus, int subscriptionId) {
    List<IncomingTextMessage> messages = new LinkedList<>();

    for (Object pdu : pdus) {
      SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
      if (msg != null){
        messages.add(new IncomingTextMessage(msg, subscriptionId));
      }
    }

    if (messages.isEmpty()) {
      return Optional.absent();
    }

    IncomingTextMessage message =  new IncomingTextMessage(messages);

    if (WirePrefix.isPrefixedMessage(message.getMessageBody())) {
      return Optional.fromNullable(multipartMessageHandler.processPotentialMultipartMessage(message));
    } else {
      return Optional.of(message);
    }
  }
}
