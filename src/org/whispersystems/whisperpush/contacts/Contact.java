package org.whispersystems.whisperpush.contacts;


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.whispersystems.textsecure.util.FutureTaskListener;
import org.whispersystems.textsecure.util.ListenableFutureTask;

import java.util.HashSet;

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
