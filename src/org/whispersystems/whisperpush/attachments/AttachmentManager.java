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
package org.whispersystems.whisperpush.attachments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.whispersystems.textsecure.internal.util.Util;

import android.content.Context;

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

    public String store(InputStream attachment) throws IOException {
        File attachmentDirectory = new File(context.getFilesDir(), "attachments");
        attachmentDirectory.mkdirs();

        File            stored = File.createTempFile("attachment", ".store", attachmentDirectory);
        FileOutputStream fout  = new FileOutputStream(stored);

        Util.copy(attachment, fout);

        return stored.getName();
    }

    public File get(String token) {
        File attachmentDirectory = new File(context.getFilesDir(), "attachments");
        return new File(attachmentDirectory, token);
    }
}
