package com.example.cataniaunited.game.board.tile_list_builder;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@QuarkusTest
class TileListDirectorTest {
    private TileListBuilder mockBuilder;
    private TileListDirector director;
        @BeforeEach
        void setUp() {
            mockBuilder = mock(TileListBuilder.class);
        }

    @Test
    void constructorThrowsNullPointerExceptionIfBuilderIsNull() {
        assertThrows(
                NullPointerException.class, () -> {
            new TileListDirector(null);
        }, "Constructor should throw NullPointerException for null builder");

    }

    @Test
    void setBuilderThrowsNullPointerExceptionIfBuilderIsNull() {
        director = new TileListDirector(mockBuilder);
            assertThrows(NullPointerException.class, () -> {
            director.setBuilder(null);
        },  "BuildSetter should throw NullPointerException for null builder");
    }

    @Test
    void setBuilderSetsInternalBuilderCorrectly() {
        director = new TileListDirector(mockBuilder);
        TileListBuilder mockBuilder2 = mock(TileListBuilder.class);
        director.setBuilder(mockBuilder2);

        assertEquals(mockBuilder2, director.builder, "Builder should be updated");
    }

    @Test
    void constructStandardTileListCallsBuilderMethodsInCorrectOrder() {

        director = new TileListDirector(mockBuilder);

        InOrder inOrder = Mockito.inOrder(mockBuilder);

        director.constructStandardTileList(3, 10, true);

        // Assert: Verify methods were called in the correct order and exactly once
        inOrder.verify(mockBuilder).reset();
        inOrder.verify(mockBuilder).setConfiguration(3, 10, true);
        inOrder.verify(mockBuilder).buildTiles();
        inOrder.verify(mockBuilder).addValues();
        inOrder.verify(mockBuilder).shuffleTiles();
        inOrder.verify(mockBuilder).assignTileIds();
        inOrder.verify(mockBuilder).calculateTilePositions();
    }

}
