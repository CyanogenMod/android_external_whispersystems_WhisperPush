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

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;

/**
 * List item view for displaying user identity keys.
 *
 * @author Moxie Marlinspike
 */
public class IdentityKeyItemView extends RelativeLayout
        implements Contact.ContactModifiedListener
{

    private TextView          identityName;
    private TextView          identityPhone;
    private TextView          fingerprint;
    private QuickContactBadge contactBadge;

    private Contact contact;
    private IdentityKey identityKey;

    private final Handler handler = new Handler();

    public IdentityKeyItemView(Context context) {
        super(context);
    }

    public IdentityKeyItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onFinishInflate() {
        this.identityName  = (TextView)findViewById(R.id.identity_name);
        this.identityPhone = (TextView)findViewById(R.id.identity_phone);
        this.fingerprint   = (TextView)findViewById(R.id.fingerprint);
        this.contactBadge  = (QuickContactBadge)findViewById(R.id.contact_photo_badge);
    }

    public void set(Pair<String, IdentityKey> identity) {
        this.contact     = ContactsFactory.getContactFromNumber(getContext(), identity.first, true);
        this.identityKey = identity.second;

        identityName.setText(contact.getNumber());
        fingerprint.setText(this.identityKey.getFingerprint());

        contactBadge.setImageBitmap(contact.getAvatar());
        contactBadge.assignContactFromPhone(contact.getNumber(), true);

        contact.addListener(this);
    }

    public IdentityKey getIdentityKey() {
        return this.identityKey;
    }

    public Contact getContact() {
        return this.contact;
    }

    @Override
    public void onModified(final Contact contact) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (contact.getName() != null) {
                    IdentityKeyItemView.this.identityName.setText(contact.getName());
                    IdentityKeyItemView.this.identityPhone.setText(contact.getNumber());
                } else {
                    IdentityKeyItemView.this.identityName.setText(contact.toShortString());
                }

                IdentityKeyItemView.this.contactBadge.setImageBitmap(contact.getAvatar());
            }
        });
    }
}
