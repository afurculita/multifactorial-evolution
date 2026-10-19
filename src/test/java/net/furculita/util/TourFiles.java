package net.furculita.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Test helper: reads a TSPLIB {@code .opt.tour} file into 0-based city indices. */
public final class TourFiles {
    private TourFiles() {
    }

    public static List<Integer> read(String path) throws IOException {
        List<Integer> tour = new ArrayList<>();
        boolean inTour = false;
        for (String raw : Files.readAllLines(Paths.get(path))) {
            String line = raw.trim();
            if (line.equalsIgnoreCase("TOUR_SECTION")) {
                inTour = true;
            } else if (inTour) {
                for (String tok : line.split("\\s+")) {
                    if (tok.isEmpty()) continue;
                    int city = Integer.parseInt(tok);
                    if (city < 0) return tour;
                    tour.add(city - 1);
                }
            }
        }
        return tour;
    }

    /** Length of the closed tour under the given distance matrix. */
    public static long closedLength(int[][] graph, List<Integer> tour) {
        long len = 0;
        for (int i = 0; i < tour.size(); i++) {
            len += graph[tour.get(i)][tour.get((i + 1) % tour.size())];
        }
        return len;
    }
}
