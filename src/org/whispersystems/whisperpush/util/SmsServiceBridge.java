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
package org.whispersystems.whisperpush.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;
import org.whispersystems.whisperpush.service.MessageNotifier;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.android.internal.telephony.ISms;

/**
 * A helper class to handle the WhisperPush --> System Framework binding.
 */
public class SmsServiceBridge {

    public static void receivedPushMessage(
            Context context, String source, Optional<String> message,
            List<Pair<String,String>> attachments, long timestampSent)
    {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method   getService     = serviceManager.getMethod("getService", String.class);
            ISms     framework      = ISms.Stub.asInterface((IBinder) getService.invoke(null, "isms"));

            if (attachments != null && attachments.size() != 0) {
                Contact contact = ContactsFactory.getContactFromNumber(context, source, false);
                MessageNotifier.notifyProblem(context, contact,
                        context.getString(R.string.SmsServiceBridge_received_encrypted_attachment));
            }

            framework.synthesizeMessages(source, null, getAsList(message), timestampSent);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e);
        } catch (RemoteException e) {
            throw new AssertionError(e);
        }
    }

    private static List<String> getAsList(Optional<String> message) {
        return Collections.singletonList(message.or(""));
    }

    @SuppressWarnings("unused") // keep for debugging
    private static void logReceived(String source, List<String> destinations, String message,
                                    List<Pair<String,String>> attachments, long timestampSent)
    {
        Log.w("SmsServiceBridge", "Incoming Message Source: " + source);

        for (String destination : destinations) {
            Log.w("SmsServiceBridge", "Incoming Message Destination: " + destination);
        }

        Log.w("SmsServiceBridge", "Incoming Message Body: " + message);

        for (Pair<String, String> attachment : attachments) {
            Log.w("SmsServiceBridge", String.format("Incoming Message Attachment: %s, %s", attachment.first, attachment.second));
        }

        Log.w("SmsServiceBridge", "Incoming Message Sent Time: " + timestampSent);
    }

}
