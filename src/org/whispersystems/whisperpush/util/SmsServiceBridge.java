package org.whispersystems.whisperpush.util;

import android.os.IBinder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import com.android.internal.telephony.ISms;

public class SmsServiceBridge {

  public static void receivedPushMessage(String source, List<String> destinations,
                                         String message, List<String> attachments,
                                         List<String> attachmentContentTypes,
                                         long timestampSent)
  {
    try {
      Class  serviceManager = Class.forName("android.os.ServiceManager");
      Method getService     = serviceManager.getMethod("getService", String.class);

      ISms.Stub.asInterface((IBinder)getService.invoke(null, "isms"));
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new AssertionError(e);
    }
  }

}
