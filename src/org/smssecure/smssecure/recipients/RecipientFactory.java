/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smssecure.smssecure.recipients;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.smssecure.smssecure.contacts.avatars.ContactPhotoFactory;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.SessionUtil;
import org.smssecure.smssecure.database.CanonicalAddressDatabase;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.RecipientPreferenceDatabase;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.libaxolotl.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class RecipientFactory {

  private static final RecipientProvider provider  = new RecipientProvider();
  private static final String TABLE                = "recipient_preferences";
  private static final String RECIPIENT_IDS_COLUMN = "recipient_ids";

  public static Recipients getRecipientsForIds(Context context, String recipientIds, boolean asynchronous) {
    if (TextUtils.isEmpty(recipientIds))
      return new Recipients();

    return getRecipientsForIds(context, Util.split(recipientIds, " "), asynchronous);
  }

  public static Recipients getRecipientsFor(Context context, List<Recipient> recipients, boolean asynchronous) {
    long[] ids = new long[recipients.size()];
    int    i   = 0;

    for (Recipient recipient : recipients) {
      ids[i++] = recipient.getRecipientId();
    }

    return provider.getRecipients(context, ids, asynchronous);
  }

  public static Recipients getRecipientsFor(Context context, Recipient recipient, boolean asynchronous) {
    long[] ids = new long[1];
    ids[0] = recipient.getRecipientId();

    return provider.getRecipients(context, ids, asynchronous);
  }

  public static Recipient getRecipientForId(Context context, long recipientId, boolean asynchronous) {
    return provider.getRecipient(context, recipientId, asynchronous);
  }

  public static Recipients getRecipientsForIds(Context context, long[] recipientIds, boolean asynchronous) {
    return provider.getRecipients(context, recipientIds, asynchronous);
  }

  public static @NonNull Recipients getRecipientsFromString(Context context, @NonNull String rawText, boolean asynchronous) {
    StringTokenizer tokenizer = new StringTokenizer(rawText, ",");
    List<String>    ids       = new LinkedList<>();

    while (tokenizer.hasMoreTokens()) {
      Optional<Long> id = getRecipientIdFromNumber(context, tokenizer.nextToken());

      if (id.isPresent()) {
        ids.add(String.valueOf(id.get()));
      }
    }

    return getRecipientsForIds(context, ids, asynchronous);
  }

  public static @NonNull Recipients getRecipientsFromStrings(@NonNull Context context, @NonNull List<String> numbers, boolean asynchronous) {
    List<String> ids = new LinkedList<>();

    for (String number : numbers) {
      Optional<Long> id = getRecipientIdFromNumber(context, number);

      if (id.isPresent()) {
        ids.add(String.valueOf(id.get()));
      }
    }

    return getRecipientsForIds(context, ids, asynchronous);
  }

  public static @NonNull Recipients getRecipientsFromXmpp(@NonNull Context context, @NonNull List<String> xmppAddresses, boolean asynchronous) {
    List<String> ids = new LinkedList<>();

    for (String xmppAddress : xmppAddresses) {
      Optional<Long> id = getRecipientIdFromXmpp(context, xmppAddress);

      if (id.isPresent()) {
        ids.add(String.valueOf(id.get()));
      }
    }

    return getRecipientsForIds(context, ids, asynchronous);
  }

  private static @NonNull Recipients getRecipientsForIds(Context context, List<String> idStrings, boolean asynchronous) {
    long[]       ids      = new long[idStrings.size()];
    int          i        = 0;

    for (String id : idStrings) {
      ids[i++] = Long.parseLong(id);
    }

    return provider.getRecipients(context, ids, asynchronous);
  }

  private static Optional<Long> getRecipientIdFromNumber(Context context, String number) {
    number = number.trim();

    if (number.isEmpty()) return Optional.absent();

    if (hasBracketedNumber(number)) {
      number = parseBracketedNumber(number);
    }

    return Optional.of(CanonicalAddressDatabase.getInstance(context).getCanonicalAddressId(number));
  }

  public static @NonNull Recipients getXmppRecipients(@NonNull Context context, boolean asynchronous) {
    return getRecipientsForIds(context, getXmppRecipientsIds(context), asynchronous);
  }

  private static List<String> getXmppRecipientsIds(Context context) {
    Cursor cursor = null;
    try {
      DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
      SQLiteDatabase db             = databaseHelper.getReadableDatabase();
      String[]       columns        = new String[]{RECIPIENT_IDS_COLUMN};
      String[]       whereArgs      = new String[]{"NULL"};
      String         where          = RecipientPreferenceDatabase.XMPP_JID + "!=?";

      cursor = db.query(TABLE, columns, where, whereArgs, null, null, null);

      List<String> recipients = new LinkedList<>();

      while (cursor != null && cursor.moveToNext()) {
        recipients.add(String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(RECIPIENT_IDS_COLUMN))));
      }

      return recipients;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  private static Optional<Long> getRecipientIdFromXmpp(Context context, String xmppAddress) {
    xmppAddress = xmppAddress.trim();

    if (xmppAddress.isEmpty()) return Optional.absent();

    Cursor cursor = null;

    try {
      DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
      SQLiteDatabase db             = databaseHelper.getReadableDatabase();
      String[]       columns        = new String[]{RECIPIENT_IDS_COLUMN};
      String[]       whereArgs      = new String[]{xmppAddress};
      String         where          = RecipientPreferenceDatabase.XMPP_JID + "=?";

      cursor = db.query(TABLE, columns, where, whereArgs, null, null, null, "1");

      if (cursor != null && cursor.moveToFirst()) {
        return Optional.of(cursor.getLong(cursor.getColumnIndexOrThrow(RECIPIENT_IDS_COLUMN)));
      }

      return Optional.absent();
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  public static @NonNull Recipients getSecureRecipients(@NonNull Context context, @NonNull MasterSecret masterSecret, boolean asynchronous) {
    List<String> addresses = CanonicalAddressDatabase.getInstance(context).getCanonicalNumbers();
    List<Long>   secureAddresses = new LinkedList<>();

    for (String address : addresses) {
      Optional<Long> addressId = getRecipientIdFromNumber(context, address);
      if (SessionUtil.hasSession(context, masterSecret, address) && addressId.isPresent()) secureAddresses.add(addressId.get());
    }

    long[] secureAddressesArray = new long[secureAddresses.size()];
    for (int i = 0; i < secureAddresses.size(); i++) secureAddressesArray[i] = secureAddresses.get(i);

    return getRecipientsForIds(context, secureAddressesArray, asynchronous);
  }

  private static boolean hasBracketedNumber(String recipient) {
    int openBracketIndex = recipient.indexOf('<');

    return (openBracketIndex != -1) &&
           (recipient.indexOf('>', openBracketIndex) != -1);
  }

  private static String parseBracketedNumber(String recipient) {
    int begin    = recipient.indexOf('<');
    int end      = recipient.indexOf('>', begin);
    String value = recipient.substring(begin + 1, end);

    return value;
  }

  public static void clearCache() {
    provider.clearCache();
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;

    public static DatabaseHelper getInstance(Context context) {
      if (instance == null) {
        instance = new DatabaseHelper(context, DatabaseFactory.DATABASE_NAME, null, DatabaseFactory.DATABASE_VERSION);
      }
      return instance;
    }

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
      super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

  }

}
