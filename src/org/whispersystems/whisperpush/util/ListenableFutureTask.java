package org.whispersystems.whisperpush.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ListenableFutureTask<V> extends FutureTask<V> {

  private FutureTaskListener<V> listener;

  public ListenableFutureTask(Callable<V> callable, FutureTaskListener<V> listener) {
    super(callable);
    this.listener = listener;
  }

  public synchronized void setListener(FutureTaskListener<V> listener) {
    this.listener = listener;

    if (this.isDone()) {
      callback();
    }
  }

  @Override
  protected synchronized void done() {
    callback();
  }

  private void callback() {
    if (this.listener != null) {
      FutureTaskListener<V> nestedListener = this.listener;
      if (nestedListener != null) {
        try {
          nestedListener.onSuccess(get());
        } catch (ExecutionException ee) {
          nestedListener.onFailure(ee);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }
}
