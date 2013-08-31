package org.whispersystems.whisperpush.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseFactory {

  private static final String DATABASE_NAME    = "whisper_push";
  private static final int    DATABASE_VERSION = 1;

  private static DatabaseFactory instance;

  private final CanonicalAddressDatabase addressDatabase;
  private final IdentityDatabase identityDatabase;

  public synchronized static DatabaseFactory getInstance(Context context) {
    if (instance == null)
      instance = new DatabaseFactory(context);

    return instance;
  }

  private DatabaseFactory(Context context) {
    DatabaseHelper databaseHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);

    this.identityDatabase = new IdentityDatabase(context, databaseHelper);
    this.addressDatabase  = new CanonicalAddressDatabase(context, databaseHelper);
  }


  public static CanonicalAddressDatabase getAddressDatabase(Context context) {
    return getInstance(context).addressDatabase;
  }

  public static IdentityDatabase getIdentityDatabase(Context context) {
    return getInstance(context).identityDatabase;
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context, String name,
                          SQLiteDatabase.CursorFactory factory,
                          int version)
    {
      super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      CanonicalAddressDatabase.onCreate(db);
      IdentityDatabase.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }
}
