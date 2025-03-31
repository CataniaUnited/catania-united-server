package game.activeGame.board;

import game.activeGame.Player;

public class Road {
    Player owner;
    final SettlementPosition positionA;
    final SettlementPosition positionB;

    public Road(SettlementPosition positionA, SettlementPosition positionB){
        this.positionA = positionA;
        this.positionB = positionB;
    }

    public SettlementPosition getNeighbour(SettlementPosition currentSettlement){
        return (this.positionA == currentSettlement) ? positionB : positionA;
    }

    @Override
    public String toString() {
        return "Road:{owner: %s; (%s, %s)}".formatted(owner, positionA.getID(), positionB.getID());
    }
}
