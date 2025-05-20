package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class SpacingRuleViolationException extends GameException {

  private static final String DEFAULT_MESSAGE = "Too close to another settlement or city";

    public SpacingRuleViolationException() {
      super(DEFAULT_MESSAGE);
    }
}
