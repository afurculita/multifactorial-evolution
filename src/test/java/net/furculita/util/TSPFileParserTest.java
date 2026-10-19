package net.furculita.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TSPFileParserTest {

    @Test
    void euc2dDistancesReproducePublishedOptimum() throws Exception {
        int[][] graph = new TSPFileParser("data/tsp/berlin52.tsp").getGraph();
        assertEquals(52, graph.length);
        assertEquals(7542, TourFiles.closedLength(graph, TourFiles.read("data/tsp/berlin52.opt.tour")));
    }

    @Test
    void geoDistancesReproducePublishedOptimum() throws Exception {
        int[][] graph = new TSPFileParser("data/tsp/ulysses16.tsp").getGraph();
        assertEquals(16, graph.length);
        assertEquals(6859, TourFiles.closedLength(graph, TourFiles.read("data/tsp/ulysses16.opt.tour")));
    }

    @Test
    void matrixIsSymmetricWithZeroDiagonal() throws Exception {
        int[][] graph = new TSPFileParser("data/tsp/burma14.tsp").getGraph();
        for (int i = 0; i < graph.length; i++) {
            assertEquals(0, graph[i][i]);
            for (int j = 0; j < graph.length; j++) {
                assertEquals(graph[i][j], graph[j][i]);
            }
        }
    }

    @Test
    void unsupportedEdgeWeightTypeIsRejected() {
        TSPException e = assertThrows(TSPException.class, () -> new TSPFileParser("data/tsp/bays29.tsp"));
        assertTrue(e.getMessage().contains("EXPLICIT"), e.getMessage());
    }

    @Test
    void missingFileIsRejected() {
        assertThrows(TSPException.class, () -> new TSPFileParser("data/tsp/does-not-exist.tsp"));
    }
}
