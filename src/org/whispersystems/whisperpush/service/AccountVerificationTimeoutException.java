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

/**
 * @author Moxie Marlinspike
 */
public class AccountVerificationTimeoutException extends Exception {
  public AccountVerificationTimeoutException() {
  }

  public AccountVerificationTimeoutException(String detailMessage) {
    super(detailMessage);
  }

  public AccountVerificationTimeoutException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public AccountVerificationTimeoutException(Throwable throwable) {
    super(throwable);
  }
}
