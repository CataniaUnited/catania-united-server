package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdk.dynalink.linker.LinkerServices;

import java.util.ArrayList;
import java.util.List;

public class Port implements Placable{
    int inputResourceAmount;
    TileType resourceToTradeWith;

    public Port(){
        inputResourceAmount = 3;
    }

    public Port(TileType portType){
        inputResourceAmount = 2;
        resourceToTradeWith = portType;
    }

    public boolean canTrade(List<TileType> inputResources, List<TileType> desiredResources){
        if (Util.isEmpty(inputResources) || Util.isEmpty(desiredResources))
            return false; // need resources to handle

        if(inputResources.size() % inputResourceAmount != 0){
            return false; // resource amount must be a multiple of trade ratio
        }

        if(inputResources.size() / inputResourceAmount != desiredResources.size()){
            return false; // desired amount must match ratio
        }


        if (this.resourceToTradeWith != null) { // It's a specific resource port
            for (TileType offeredResource : inputResources) {
                if (offeredResource != this.resourceToTradeWith) {
                    return false; // Player is offering the wrong type of resource for this specific port.
                }
            }
        }


        List<TileType> inputResourceClone = new ArrayList<>(List.copyOf(inputResources));
        int goalAmount = desiredResources.size();
        while (!inputResourceClone.isEmpty()){
            TileType resource = inputResourceClone.get(0);
            int amountOfFirstResource = 0;

            for (TileType t : inputResourceClone) {
                if (t == resource) {
                    amountOfFirstResource++;
                }
            }
            inputResourceClone.removeIf(t -> t == resource);

            if (amountOfFirstResource % inputResourceAmount != 0){
                return false;
            }
            goalAmount -= amountOfFirstResource / inputResourceAmount;
        }


        return desiredResources.stream().noneMatch(inputResources::contains);
    }

    @Override
    public double[] getCoordinates() {
        return new double[0];
    }

    @Override
    public ObjectNode toJson() {
        return null;
    }
}
