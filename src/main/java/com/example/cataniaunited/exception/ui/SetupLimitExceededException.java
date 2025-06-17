package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class SetupLimitExceededException extends GameException {
  private static final String DEFAULT_MESSAGE = "Only one settlement and one road may be placed per player during this setup round.";
  public SetupLimitExceededException() {
    super(DEFAULT_MESSAGE);
  }
}
