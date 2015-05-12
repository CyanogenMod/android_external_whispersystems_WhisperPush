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
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.PendingApprovalDatabase;
import org.whispersystems.whisperpush.loaders.PendingLoader;

public class VerifyIdentitiesActivity extends ListActivity
        implements LoaderManager.LoaderCallbacks<Cursor>
{

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.verify_identities_activity);

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
        Intent viewIntent = new Intent(this, VerifyIdentityActivity.class);
        viewIntent.putExtra("identity_key", ((PendingIdentityItemView)view).getIdentityKey().serialize());
        viewIntent.putExtra("contact", ((PendingIdentityItemView)view).getContact());
        startActivity(viewIntent);
    }

    private void initializeListAdapter() {
        this.setListAdapter(new PendingListAdapter(this, null));
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PendingLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ((CursorAdapter)getListAdapter()).changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorAdapter)getListAdapter()).changeCursor(null);
    }

    private class PendingListAdapter extends CursorAdapter {
        private final LayoutInflater inflater;

        public PendingListAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            PendingApprovalDatabase.Reader reader = DatabaseFactory.getPendingApprovalDatabase(context)
                    .readerFor(cursor);

            ((PendingIdentityItemView)view).set(reader.getCurrent());
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.pending_identity_item_view, parent, false);
        }
    }
}