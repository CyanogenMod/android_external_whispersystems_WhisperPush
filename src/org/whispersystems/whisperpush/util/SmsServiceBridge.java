/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.whispersystems.whisperpush.util;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import com.android.internal.telephony.ISms;

/**
 * A helper class to handle the WhisperPush --> System Framework binding.
 */
public class SmsServiceBridge {

  public static void receivedPushMessage(String source, List<String> destinations,
                                         String message, List<String> attachments,
                                         List<String> attachmentContentTypes,
                                         long timestampSent)
  {
    try {
      Class  serviceManager = Class.forName("android.os.ServiceManager");
      Method getService     = serviceManager.getMethod("getService", String.class);
      ISms   framework      = ISms.Stub.asInterface((IBinder) getService.invoke(null, "isms"));

      logReceived(source, destinations, message, attachments, attachmentContentTypes, timestampSent);
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

  private static List<String> getAsList(String message) {
    List<String> messages = new LinkedList<String>();
    messages.add(message == null ? "" : message);
    return messages;
  }

  private static void logReceived(String source, List<String> destinations, String message,
                                  List<String> attachments, List<String> attachmentContentTypes,
                                  long timestampSent)
  {
    Log.w("SmsServiceBridge", "Incoming Message Source: " + source);

    for (String destination : destinations) {
      Log.w("SmsServiceBridge", "Incoming Message Destination: " + destination);
    }

    Log.w("SmsServiceBridge", "Incoming Message Body: " + message);

    for (String attachment : attachments) {
      Log.w("SmsServiceBridge", "Incoming Message Attachment: " + attachment);
    }

    Log.w("SmsServiceBridge", "Incoming Message Sent Time: " + timestampSent);
  }

}
