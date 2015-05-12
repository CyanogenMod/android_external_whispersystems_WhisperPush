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
package org.whispersystems.whisperpush.contacts;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.util.LruCache;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.util.LinkedBlockingLifoQueue;
import org.whispersystems.whisperpush.util.ListenableFutureTask;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ContactsFactory {

    private static final String[] CALLER_ID_PROJECTION = new String[] {
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.LOOKUP_KEY,
            PhoneLookup._ID,
    };

    private static final LruCache<String,Contact> contactCache  = new LruCache<String, Contact>(1000);
    private static final ExecutorService asyncRecipientResolver = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingLifoQueue<Runnable>());

    private static Bitmap defaultContactPhoto;

    public static Contact getContactFromNumber(Context context, String number, boolean asynchronous) {
        Contact cachedContact = contactCache.get(number);

        if      (cachedContact != null) return cachedContact;
        else if (asynchronous)          return getAsynchronousContactFromNumber(context, number);
        else                            return getSynchronousContactFromNumber(context, number);
    }

    private static Contact getSynchronousContactFromNumber(Context context, String number) {
        ContactDetails contactDetails = getContactDetailsFromNumber(context, number);
        Contact        contact        = new Contact(number, contactDetails.name,
                contactDetails.avatar,
                contactDetails.contactUri);
        contactCache.put(number, contact);

        return contact;
    }

    private static Contact getAsynchronousContactFromNumber(final Context context, final String number) {
        Callable<ContactDetails> task = new Callable<ContactDetails>() {
            @Override
            public ContactDetails call() throws Exception {
                return getContactDetailsFromNumber(context, number);
            }
        };

        ListenableFutureTask<ContactDetails> future = new ListenableFutureTask<ContactDetails>(task, null);

        asyncRecipientResolver.submit(future);

        Contact contact = new Contact(number, getDefaultContactPhoto(context), future);
        contactCache.put(number, contact);

        return contact;
    }

    private static ContactDetails getContactDetailsFromNumber(Context context, String number) {
        Uri uri       = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = context.getContentResolver().query(uri, CALLER_ID_PROJECTION, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                Uri contactUri      = Contacts.getLookupUri(cursor.getLong(2), cursor.getString(1));
                Bitmap contactPhoto = getContactPhoto(context, Uri.withAppendedPath(Contacts.CONTENT_URI,
                        cursor.getLong(2)+""));

                return new ContactDetails(cursor.getString(0), contactUri, contactPhoto);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return new ContactDetails(null, null, getDefaultContactPhoto(context));
    }

    private static Bitmap getContactPhoto(Context context, Uri uri) {
        InputStream inputStream = Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);

        if (inputStream == null) return getDefaultContactPhoto(context);
        else                     return BitmapFactory.decodeStream(inputStream);
    }

    private synchronized static Bitmap getDefaultContactPhoto(Context context) {
        if (defaultContactPhoto == null)
            defaultContactPhoto =  BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_contact_picture);
        return defaultContactPhoto;
    }

    public static class ContactDetails {
        public final String name;
        public final Bitmap avatar;
        public final Uri contactUri;

        public ContactDetails(String name, Uri contactUri, Bitmap avatar) {
            this.name       = name;
            this.avatar     = avatar;
            this.contactUri = contactUri;
        }
    }
}
