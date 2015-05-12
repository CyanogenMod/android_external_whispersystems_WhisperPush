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

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.libaxolotl.InvalidVersionException;
import org.whispersystems.libaxolotl.protocol.PreKeyWhisperMessage;
import org.whispersystems.textsecure.api.messages.TextSecureEnvelope;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;

import android.content.Context;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class PendingIdentityItemView extends RelativeLayout
        implements Contact.ContactModifiedListener
{

    private TextView          identityName;
    private TextView          details;
    private QuickContactBadge contactBadge;

    private Contact contact;
    private IdentityKey identityKey;

    private final Handler handler = new Handler();

    public PendingIdentityItemView(Context context) {
        super(context);
    }

    public PendingIdentityItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onFinishInflate() {
        this.identityName = (TextView         ) findViewById(R.id.identity_name);
        this.details      = (TextView         ) findViewById(R.id.pending_details);
        this.contactBadge = (QuickContactBadge) findViewById(R.id.contact_photo_badge);
    }

    public void set(TextSecureEnvelope message) {
        try {
            this.contact     = ContactsFactory.getContactFromNumber(getContext(), message.getSource(), true);
            this.identityKey = new PreKeyWhisperMessage(message.getMessage()).getIdentityKey();

            identityName.setText(contact.getNumber());
            details.setText(String.format(getContext().getString(R.string.PendingIdentityItemView_received_s),
                    DateUtils.getRelativeTimeSpanString(getContext(),
                            message.getTimestamp(),
                            false)));
            contactBadge.setImageBitmap(contact.getAvatar());
            contactBadge.assignContactFromPhone(contact.getNumber(), true);

            contact.addListener(this);
        } catch (InvalidVersionException e) {
            throw new AssertionError(e);
        } catch (InvalidMessageException e) {
            throw new AssertionError(e);
        }
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
                PendingIdentityItemView.this.identityName.setText(contact.toShortString());
                PendingIdentityItemView.this.contactBadge.setImageBitmap(contact.getAvatar());
            }
        });
    }
}
