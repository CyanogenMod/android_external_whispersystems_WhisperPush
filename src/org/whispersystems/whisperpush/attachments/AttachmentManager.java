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
package org.whispersystems.whisperpush.attachments;

import android.content.Context;

import org.whispersystems.textsecure.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The manager responsible for storing and retrieving received attachments.
 *
 * @author Moxie Marlinspike
 */
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
