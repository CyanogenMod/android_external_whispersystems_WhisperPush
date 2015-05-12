package org.whispersystems.whisperpush.util;

public interface FutureTaskListener<V> {
  public void onSuccess(V result);
  public void onFailure(Throwable error);
}
