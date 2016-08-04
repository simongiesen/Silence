package org.smssecure.smssecure.mms;

public class MmsSendResult {

  private final byte[]  messageId;
  private final int     responseStatus;
  private final boolean upgradedSecure;
  private final boolean push;

  public MmsSendResult(byte[] messageId, int responseStatus, boolean upgradedSecure, boolean push) {
    this.messageId      = messageId;
    this.responseStatus = responseStatus;
    this.upgradedSecure = upgradedSecure;
    this.push           = false;
  }

  public boolean isUpgradedSecure() {
    return upgradedSecure;
  }

  public int getResponseStatus() {
    return responseStatus;
  }

  public byte[] getMessageId() {
    return messageId;
  }
}
