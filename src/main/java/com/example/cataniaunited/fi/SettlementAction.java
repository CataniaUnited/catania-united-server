package com.example.cataniaunited.fi;

import com.example.cataniaunited.exception.GameException;

@FunctionalInterface
public interface SettlementAction {
    void execute(int settlementPositionId) throws GameException;
}
