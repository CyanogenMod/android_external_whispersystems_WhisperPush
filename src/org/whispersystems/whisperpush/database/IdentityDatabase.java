package org.whispersystems.whisperpush.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.InvalidKeyException;
import org.whispersystems.textsecure.crypto.MasterCipher;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.storage.CanonicalRecipientAddress;
import org.whispersystems.textsecure.util.Base64;

import java.io.IOException;

public class IdentityDatabase {

  private static final String TABLE_NAME   = "identities";
  private static final String ID           = "_id";
  public  static final String ADDRESS      = "address";
  public  static final String IDENTITY_KEY = "key";
  public  static final String MAC          = "mac";

  private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
                                             " (" + ID + " INTEGER PRIMARY KEY, " +
                                             ADDRESS + " INTEGER UNIQUE, " +
                                             IDENTITY_KEY + " TEXT, " +
                                             MAC + " TEXT);";

  private static final String CREATE_INDEX  = "CREATE INDEX IF NOT EXISTS address_index ON " +
                                              TABLE_NAME + " (" + ADDRESS + ");";

  private final Context context;
  private final SQLiteOpenHelper databaseHelper;

  public IdentityDatabase(Context context, SQLiteOpenHelper databaseHelper) {
    this.context        = context;
    this.databaseHelper = databaseHelper;
  }

  public boolean isValidIdentity(MasterSecret masterSecret,
                                 CanonicalRecipientAddress address,
                                 IdentityKey theirIdentity)
  {
    SQLiteDatabase database     = databaseHelper.getReadableDatabase();
    MasterCipher   masterCipher = new MasterCipher(masterSecret);
    long           addressId    = address.getCanonicalAddress(context);
    Cursor         cursor       = null;

    try {
      cursor = database.query(TABLE_NAME, null, ADDRESS + " = ?",
                              new String[] {addressId+""}, null, null,null);

      if (cursor != null && cursor.moveToFirst()) {
        String serializedIdentity = cursor.getString(cursor.getColumnIndexOrThrow(IDENTITY_KEY));
        String mac                = cursor.getString(cursor.getColumnIndexOrThrow(MAC));

        if (!masterCipher.verifyMacFor(addressId + serializedIdentity, Base64.decode(mac))) {
          Log.w("IdentityDatabase", "MAC failed");
          return false;
        }

        IdentityKey ourIdentity = new IdentityKey(Base64.decode(serializedIdentity), 0);
        return ourIdentity.equals(theirIdentity);
      } else {
        return true;
      }
    } catch (IOException e) {
      Log.w("IdentityDatabase", e);
      return false;
    } catch (InvalidKeyException e) {
      Log.w("IdentityDatabase", e);
      return false;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  public void saveIdentity(MasterSecret masterSecret,
                           CanonicalRecipientAddress address,
                           IdentityKey identityKey)
  {
    SQLiteDatabase database   = databaseHelper.getWritableDatabase();
    long addressId = address.getCanonicalAddress(context);
    MasterCipher masterCipher = new MasterCipher(masterSecret);
    String identityKeyString  = Base64.encodeBytes(identityKey.serialize());
    String macString          = Base64.encodeBytes(masterCipher.getMacFor(addressId +
                                                                          identityKeyString));

    ContentValues contentValues = new ContentValues();
    contentValues.put(ADDRESS, addressId);
    contentValues.put(IDENTITY_KEY, identityKeyString);
    contentValues.put(MAC, macString);

    database.replace(TABLE_NAME, null, contentValues);
  }

  public static void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_TABLE);
    db.execSQL(CREATE_INDEX);
  }
}
