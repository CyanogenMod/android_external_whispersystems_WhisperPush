package org.whispersystems.whisperpush.loaders;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;

import org.whispersystems.whisperpush.database.DatabaseFactory;

public class IdentityLoader extends CursorLoader {

  private final Context context;

  public IdentityLoader(Context context) {
    super(context);
    this.context      = context.getApplicationContext();
  }

  @Override
  public Cursor loadInBackground() {
    return DatabaseFactory.getIdentityDatabase(context).getIdentities();
  }

}
