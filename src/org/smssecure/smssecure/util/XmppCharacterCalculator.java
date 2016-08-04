package org.smssecure.smssecure.util;

public class XmppCharacterCalculator extends CharacterCalculator {
  private static final int MAX_SIZE = 2000;
  @Override
  public CharacterState calculateCharacters(int charactersSpent) {
    return new CharacterState(1, MAX_SIZE - charactersSpent, MAX_SIZE);
  }
}
