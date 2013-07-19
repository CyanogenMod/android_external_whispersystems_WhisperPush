package org.whispersystems.whisperpush.sms;


import android.content.BroadcastReceiver.PendingResult;
import android.content.Intent;

import java.util.LinkedList;

public class OutgoingSmsQueue {

  private static final OutgoingSmsQueue instance = new OutgoingSmsQueue();

  public static OutgoingSmsQueue getInstance() {
    return instance;
  }

  private final LinkedList<OutgoingMessageCandidate> queue = new LinkedList<OutgoingMessageCandidate>();

  public synchronized void put(OutgoingMessageCandidate candidate) {
    queue.add(candidate);
  }

  public synchronized OutgoingMessageCandidate get() {
    if (queue.isEmpty())
      return null;

    return queue.removeFirst();
  }

  public static class OutgoingMessageCandidate {

    private final Intent        intent;
    private final PendingResult pendingResult;

    public OutgoingMessageCandidate(Intent intent, PendingResult pendingResult) {
      this.intent        = intent;
      this.pendingResult = pendingResult;
    }

    public Intent getIntent() {
      return intent;
    }

    public PendingResult getPendingResult() {
      return pendingResult;
    }
  }
}
