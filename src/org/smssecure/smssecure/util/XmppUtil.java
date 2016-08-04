package org.smssecure.smssecure.util;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.service.KeyCachingService;
import org.smssecure.smssecure.service.XmppService;
import org.smssecure.smssecure.sms.MessageSender;
import org.smssecure.smssecure.sms.OutgoingTextMessage;
import org.smssecure.smssecure.sms.OutgoingXmppExchangeMessage;
import org.smssecure.smssecure.util.Base64;
import org.smssecure.smssecure.util.dualsim.SubscriptionManagerCompat;
import org.smssecure.smssecure.util.SilencePreferences;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

public class XmppUtil {
  private static final String TAG = XmppUtil.class.getSimpleName();

  public  static Integer XMPP_TIMEOUT        = 10000; // 10s
  public  static Integer XMPP_STANZA_TIMEOUT = 30000; // 30s
  private static ArrayList<XmppServer> xmppServers = new ArrayList<XmppServer>() {
    {
      add(new XmppServer("xmpp.silence.im"));
    }
  };

  public static void tryToRegister(Context context, ArrayList<XmppServer> serversToTest)
    throws Exception
  {
    if (!SilencePreferences.isXmppRegistered(context)) {
      if (serversToTest.size() <= 0) throw new Exception("No more XMPP server available");

      Random random = new Random();
      Integer randomInt = random.nextInt(serversToTest.size());
      XmppServer xmppServerToRegister = serversToTest.get(randomInt);
      Log.w(TAG, "Registering on server " + xmppServerToRegister.getHostname());
      try {
        register(context, xmppServerToRegister);
      } catch (Exception e) {
        Log.w(TAG, e);
        serversToTest.remove(xmppServerToRegister);
        tryToRegister(context, serversToTest);
      }
    }
  }

  public static void tryToRegister(Context context)
    throws Exception
  {
    tryToRegister(context, new ArrayList<XmppServer>(getXmppServers()));
  }

  public static void register(Context context, XmppServer xmppServerToRegister)
    throws IOException, SmackException, SmackException.NoResponseException, XMPPException, XMPPException.XMPPErrorException
  {
    if (!SilencePreferences.isXmppRegistered(context)) {
      stopService(context);

      String uuid = UUID.randomUUID().toString();
      String password;
      try {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt               = new byte[64];
        secureRandom.nextBytes(salt);
        password = Base64.encodeBytes(salt).replaceAll("[=*$]","");
      } catch (Exception e) {
        Log.w(TAG, e);
        throw new AssertionError(e);
      }

      Log.w(TAG, "Registering with UUID " + uuid);

      XMPPTCPConnectionConfiguration.Builder configBuilder = getConfigBuilder(context);
      configBuilder.setServiceName(xmppServerToRegister.getHostname())
                   .setPort(xmppServerToRegister.getPort());
      XMPPTCPConnection connection = new XMPPTCPConnection(configBuilder.build());
      connection.setPacketReplyTimeout(XMPP_STANZA_TIMEOUT);
      connection.connect();

      AccountManager accountManager = AccountManager.getInstance(connection);
      if (!accountManager.supportsAccountCreation()) {
        throw new UnsupportedOperationException("Account registation is not supported");
      }
      if (accountManager.getAccountAttributes().size() > 2) {
        throw new UnsupportedOperationException("Anonymous account registation is not supported");
      }
      accountManager.createAccount(uuid, password);

      SilencePreferences.setXmppUsername(context, uuid);
      SilencePreferences.setXmppPassword(context, password);
      SilencePreferences.setXmppHostname(context, xmppServerToRegister.getHostname());
      SilencePreferences.setXmppPort(context, xmppServerToRegister.getPort());

      connection.disconnect();
    }
  }

  public static XMPPTCPConnectionConfiguration.Builder getConfigBuilder(Context context) {
    XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
    configBuilder.setResource(context.getString(R.string.app_name))
                 .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                 .setEnabledSSLProtocols(new String[] { "TLSv1",  "TLSv1.1", "TLSv1.2" })
                 .setConnectTimeout(XMPP_TIMEOUT)
                 .setDebuggerEnabled(true);
    return configBuilder;
  }

  public static void startService(Context context) {
    context.startService(new Intent(context, XmppService.class));
  }

  public static void stopService(Context context) {
    context.stopService(new Intent(context, XmppService.class));
  }

  public static @NonNull ArrayList<XmppServer> getXmppServers() {
    return xmppServers;
  }

  public static @Nullable XmppServer getRegisteredServer(Context context) {
    if (SilencePreferences.isXmppRegistered(context)) {
      return new XmppServer(SilencePreferences.getXmppUsername(context),
                            SilencePreferences.getXmppPassword(context),
                            SilencePreferences.getXmppHostname(context),
                            SilencePreferences.getXmppPort(context));
    } else {
      return null;
    }
  }

  public static boolean isXmppAvailable(Context context) {
    XmppService instance = XmppService.getInstance();
    return SilencePreferences.isXmppRegistered(context) &&
           instance != null                             &&
           instance.isConnected();
  }

  public static void sendNullXmppMessage(final Context context) {
    MasterSecret masterSecret = KeyCachingService.getMasterSecret(context);
    if (masterSecret == null) throw new AssertionError("null masterSecret");

    new AsyncTask<MasterSecret, Void, Void>() {
      @Override
      protected Void doInBackground(MasterSecret... params) {
        for (Recipient recipient : RecipientFactory.getXmppRecipients(context, false)) {
          Recipients recipients = RecipientFactory.getRecipientsFor(context, recipient, false);
          OutgoingXmppExchangeMessage xmppExchangeMessage =
              new OutgoingXmppExchangeMessage(recipients, "NULL", SubscriptionManagerCompat.getDefaultMessagingSubscriptionId().or(-1));
          MessageSender.send(context, params[0], xmppExchangeMessage, -1, false);
        }

        return null;
      }
    }.execute(masterSecret);
  }

  public static class XmppServer {
    private String  username;
    private String  password;
    private String  hostname;
    private Integer port;

    public XmppServer(String username, String password, String hostname, Integer port){
      this.username = username;
      this.password = password;
      this.hostname = hostname;
      this.port = port;
    }

    public XmppServer(String username, String password, String hostname){
      this.username = username;
      this.password = password;
      this.hostname = hostname;
      this.port = 5222;
    }

    public XmppServer(String hostname, Integer port) {
      this.username = null;
      this.password = null;
      this.hostname = hostname;
      this.port = port;
    }

    public XmppServer(String hostname) {
      this.hostname = hostname;
      this.port = 5222;
    }

    public String getUsername() {
      return this.username;
    }

    public String getPassword() {
      return this.password;
    }

    public String getHostname() {
      return this.hostname;
    }

    public Integer getPort() {
      return this.port;
    }

    public String toString() {
      return this.hostname + ":" + this.port;
    }
  }

}
