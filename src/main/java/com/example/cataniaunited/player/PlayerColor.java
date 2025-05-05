package com.example.cataniaunited.player;

public enum PlayerColor {
    RED("#BD2828"),
    LIGHT_RED("FF9999"),
    PURPLE("#B62FB2"),
    LAVENDER("#CCCCFF"),
    BLUE("#0080B3"),
    LIGHT_BLUE("#B1DAE7"),
    GREEN("#008000"),
    LIGHT_GREEN("#A7F1A7"),
    ORANGE("#CF6D17"),
    LIGHT_ORANGE("#FFCC99"),
    YELLOW("#DB9224"),
    GREY("#999999");

    private final String hexCode;
    PlayerColor(String hexCode) {
        this.hexCode = hexCode;
    }

    public String getHexCode(){
        return hexCode;
    }
}
