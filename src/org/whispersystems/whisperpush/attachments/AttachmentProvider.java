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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A Content Provider that the system framework can query to retrieve attachment
 * media associated with incoming messages.
 *
 * @author Moxie Marlinspike
 */
public class AttachmentProvider extends ContentProvider {

  public static final String CONTENT_URI = "org.whispersystems.whisperpush.provider";

  private static final int ATTACHMENT = 1;

  private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

  static {
    matcher.addURI(CONTENT_URI, "attachment/*", ATTACHMENT);
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
      throws FileNotFoundException
  {
    switch (matcher.match(uri)) {
      case ATTACHMENT:
        File attachment = getAttachmentForUri(uri);
        return ParcelFileDescriptor.open(attachment, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    return super.openFile(uri, mode);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    try {
      switch (matcher.match(uri)) {
        case ATTACHMENT:
          File attachment = getAttachmentForUri(uri);

          if (attachment.delete()) return 1;
          else                     return 0;
      }
    } catch (FileNotFoundException e) {
      Log.w("AttachmentProvider", e);
    }

    return 0;
  }

  @Override
  public String getType(Uri uri) {
    switch (matcher.match(uri)) {
      case ATTACHMENT: return "application/octet-stream";
    }

    return null;
  }

  @Override
  public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
    switch (matcher.match(uri)) {
      case ATTACHMENT:
        return new String[]{"application/octet-stream"};
    }
    return null;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder)
  {
    return null;
  }


  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }

  private File getAttachmentForUri(Uri uri) throws FileNotFoundException {
    String attachmentName = uri.getLastPathSegment();

    if (attachmentName == null)
      throw new FileNotFoundException("No path segment on URI");

    return AttachmentManager.getInstance(getContext()).get(attachmentName);
  }
}
