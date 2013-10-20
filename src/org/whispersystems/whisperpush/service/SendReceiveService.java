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
package org.whispersystems.whisperpush.service;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue.OutgoingMessageCandidate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The service that does the work of delivering outgoing messages and processing
 * incoming messages.
 *
 * @author Moxie Marlinspike
 */
public class SendReceiveService extends Service {

  public static final String RECEIVE_SMS = "org.whispersystems.SendReceiveService.RECEIVE_SMS";
  public static final String SEND_SMS    = "org.whispersystems.SendReceiveService.SEND_SMS";

  public  static final String DESTINATION  = "destAddr";

  private final ExecutorService  executor    = Executors.newCachedThreadPool();
  private final OutgoingSmsQueue outgoingQuue = OutgoingSmsQueue.getInstance();

  private MessageSender   messageSender;
  private MessageReceiver messageReceiver;

  @Override
  public void onCreate() {
    this.messageSender   = new MessageSender(this);
    this.messageReceiver = new MessageReceiver(this);
  }


  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    if (intent != null) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          if (RECEIVE_SMS.equals(intent.getAction())) {
            IncomingPushMessage message = intent.getParcelableExtra("message");
            messageReceiver.handleReceiveMessage(message);
          } else if (SEND_SMS.equals(intent.getAction())) {
            OutgoingMessageCandidate message = outgoingQuue.get();
            messageSender.handleSendMessage(message);
          }
        }
      });
    }

    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

}
