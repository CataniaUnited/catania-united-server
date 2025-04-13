package com.example.cataniaunited.player;

public enum PlayerColor {
    NONE("#CCCCCC"),
    RED("#BD2828"),
    BLUE("#0080B3"),
    YELLOW("#DB9224"),
    PURPLE("#B62FB2"),
    GREEN("#008000"),
    ORANGE("#CF6D17");

    private final String hexCode;
    PlayerColor(String hexCode) {
        this.hexCode = hexCode;
    }

    public String getHexCode(){
        return hexCode;
    }
}
