package org.whispersystems.whisperpush.attachments;

import android.content.Context;

import org.whispersystems.textsecure.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AttachmentManager {

  private static AttachmentManager instance;

  public static synchronized AttachmentManager getInstance(Context context) {
    if (instance == null)
      instance = new AttachmentManager(context);

    return instance;
  }

  private final Context context;

  private AttachmentManager(Context context) {
    this.context = context;
  }

  public String store(File attachment) throws IOException {
    File attachmentDirectory = new File(context.getFilesDir(), "attachments");
    attachmentDirectory.mkdirs();

    File            stored = File.createTempFile("attachment", ".store", attachmentDirectory);
    FileInputStream fin    = new FileInputStream(attachment);
    FileOutputStream fout  = new FileOutputStream(stored);

    Util.copy(fin, fout);

    return attachment.getName();
  }

  public File get(String token) {
    File attachmentDirectory = new File(context.getFilesDir(), "attachments");
    return new File(attachmentDirectory, token);
  }
}
