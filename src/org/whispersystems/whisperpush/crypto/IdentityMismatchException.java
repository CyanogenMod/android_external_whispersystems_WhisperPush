package org.whispersystems.whisperpush.crypto;

import java.io.IOException;

public class IdentityMismatchException extends IOException {
  public IdentityMismatchException(String s) {
    super(s);
  }
}
