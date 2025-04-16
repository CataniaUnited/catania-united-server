package com.example.cataniaunited.game.board;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Placable {
    double[] getCoordinates();
    ObjectNode toJson();
}
