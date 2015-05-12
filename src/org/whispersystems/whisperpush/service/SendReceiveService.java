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
package org.whispersystems.whisperpush.service;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.whispersystems.textsecure.api.messages.TextSecureEnvelope;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.PendingApprovalDatabase;
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

    public static final String RCV_NOTIFICATION = "org.whispersystems.SendReceiveService.RCV_NOTIFICATION";
    public static final String RCV_PENDING      = "org.whispersystems.SendReceiveService.RCV_PENDING";
    public static final String SEND_SMS         = "org.whispersystems.SendReceiveService.SEND_SMS";

    public  static final String DESTINATION  = "destAddr";

    private final ExecutorService  executor    = Executors.newCachedThreadPool();
    private final OutgoingSmsQueue outgoingQueue = OutgoingSmsQueue.getInstance();

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
                    String action = intent.getAction();
                    if (RCV_NOTIFICATION.equals(action)) {
                        messageReceiver.handleNotification();
                    } else if (RCV_PENDING.equals(action)) {
                        long messageId = intent.getLongExtra("message_id", 0);
                        PendingApprovalDatabase database = DatabaseFactory.
                                getPendingApprovalDatabase(getApplicationContext());
                        TextSecureEnvelope message = database.get(messageId);
                        if(message != null) {
                            database.delete(messageId);
                            messageReceiver.handleEnvelope(message, true);
                        }
                    } else if (SEND_SMS.equals(action)) {
                        OutgoingMessageCandidate message = outgoingQueue.get();
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