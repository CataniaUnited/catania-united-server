package com.example.cataniaunited.fi;

import com.example.cataniaunited.exception.GameException;

@FunctionalInterface
public interface BuildingAction {
    void execute(int buildingSitePositionId) throws GameException;
}
