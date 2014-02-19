/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.internal.telephony;

interface ISms {

/** Interface for applications to access the SMS Manager Proxy.
 *
 * <p>The following code snippet demonstrates a static method to
 * retrieve the ISms interface from Android:</p>
 * <pre>private static ISms getSmsInterface()
            throws DeadObjectException {
    Class sm = Class.forName("android.os.ServiceManager");
    Method getService = sm.getMethod("getService", String.class);
    smsTransport = ISms.Stub.asInterface((IBinder)getService.invoke(null, "isms"));
}
 * </pre>
 */

/*
  void receivedPushMessage(in String originatingAddress, in List<String> destinations,
                           in String message, in List<String> attachments,
                           in List<String> attachmentContentTypes, long timestampMillis);
                           */
      void synthesizeMessages(String originatingAddress, String scAddress,
                          in List<String> messages, long timestampMillis);
}
