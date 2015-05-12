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


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.HashSet;

import org.whispersystems.whisperpush.util.FutureTaskListener;
import org.whispersystems.whisperpush.util.ListenableFutureTask;

public class Contact implements Parcelable {

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    private final HashSet<ContactModifiedListener> listeners = new HashSet<ContactModifiedListener>();

    private String number;
    private String name;
    private Bitmap avatar;
    private Uri    contactUri;

    public Contact(String number, String name, Bitmap avatar, Uri contactUri) {
        this.number     = number;
        this.name       = name;
        this.avatar     = avatar;
        this.contactUri = contactUri;
    }

    public Contact(String number, Bitmap avatar,
                   ListenableFutureTask<ContactsFactory.ContactDetails> future)
    {
        this.number = number;
        this.avatar = avatar;

        future.setListener(new FutureTaskListener<ContactsFactory.ContactDetails>() {
            @Override
            public void onSuccess(ContactsFactory.ContactDetails result) {
                if (result != null) {
                    HashSet<ContactModifiedListener> localListeners;

                    synchronized (Contact.this) {
                        Contact.this.name       = result.name;
                        Contact.this.contactUri = result.contactUri;
                        Contact.this.avatar     = result.avatar;
                        localListeners          = (HashSet<ContactModifiedListener>)listeners.clone();
                        listeners.clear();
                    }

                    for (ContactModifiedListener listener : localListeners) {
                        listener.onModified(Contact.this);
                    }
                }
            }

            @Override
            public void onFailure(Throwable error) {
                Log.w("Contact", error);
            }
        });
    }

    public Contact(Parcel in) {
        this.number     = in.readString();
        this.name       = in.readString();
        this.contactUri = (Uri   ) in.readParcelable(null);
        this.avatar     = (Bitmap) in.readParcelable(null);
    }

    public synchronized String getNumber() {
        return number;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized Bitmap getAvatar() {
        return avatar;
    }

    public synchronized Uri getContactUri() {
        return contactUri;
    }

    public synchronized String toShortString() {
        return (name == null ? number : name);
    }

    public synchronized void addListener(ContactModifiedListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(ContactModifiedListener listener) {
        listeners.remove(listener);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public synchronized void writeToParcel(Parcel dest, int flags) {
        dest.writeString(number);
        dest.writeString(name);
        dest.writeParcelable(contactUri, 0);
        dest.writeParcelable(avatar, 0);
    }

    public static interface ContactModifiedListener {
        public void onModified(Contact recipient);
    }
}
