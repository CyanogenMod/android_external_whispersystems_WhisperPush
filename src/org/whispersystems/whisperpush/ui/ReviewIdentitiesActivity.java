/**
 * Copyright (C) 2013 The CyanogenMod Project
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
package org.whispersystems.whisperpush.ui;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.crypto.MasterSecret;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.IdentityDatabase;
import org.whispersystems.whisperpush.loaders.IdentityLoader;

public class ReviewIdentitiesActivity extends ListActivity
        implements LoaderManager.LoaderCallbacks<Cursor>
{

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.review_identities_activity);

        initializeListAdapter();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
        }

        return false;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Intent viewIntent = new Intent(this, ViewIdentityActivity.class);
        viewIntent.putExtra("identity_key", ((IdentityKeyItemView)view).getIdentityKey().serialize());
        startActivity(viewIntent);
    }

    private void initializeListAdapter() {
        MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(this);
        this.setListAdapter(new IdentitiesListAdapter(this, null, masterSecret));
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new IdentityLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ((CursorAdapter)getListAdapter()).changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorAdapter)getListAdapter()).changeCursor(null);
    }

    private class IdentitiesListAdapter extends CursorAdapter {
        private final MasterSecret   masterSecret;
        private final LayoutInflater inflater;

        public IdentitiesListAdapter(Context context, Cursor cursor, MasterSecret masterSecret) {
            super(context, cursor);
            this.masterSecret = masterSecret;
            this.inflater     = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            IdentityDatabase.Reader reader = DatabaseFactory.getIdentityDatabase(context)
                    .readerFor(masterSecret, cursor);

            ((IdentityKeyItemView)view).set(reader.getCurrent());
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.identity_key_item_view, parent, false);
        }
    }
}
