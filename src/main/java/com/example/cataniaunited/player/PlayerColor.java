package com.example.cataniaunited.player;

public enum PlayerColor {
    RED("#BD2828"),
    LIGHT_RED("#FF9999"),
    PURPLE("#B62FB2"),
    LAVENDER("#CCCCFF"),
    BLUE("#0080B3"),
    DARK_BLUE("#002D72"),
    GREEN("#008000"),
    LIGHT_GREEN("#A7F1A7"),
    ORANGE("#FF6A00"),
    LIGHT_ORANGE("#FF944D"),
    YELLOW("#DB9224"),
    GREY("#999999"),
    BRIGHT_YELLOW("#FFF200"),
    CYAN("#00FFFF"),
    MAGENTA("#FF00FF"),
    TEAL("#008080"),
    BROWN("#8B4513");

    private final String hexCode;
    PlayerColor(String hexCode) {
        this.hexCode = hexCode;
    }

    public String getHexCode(){
        return hexCode;
    }
}
