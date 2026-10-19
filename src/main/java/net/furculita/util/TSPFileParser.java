package net.furculita.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Reads a TSPLIB {@code .tsp} file and builds its full distance matrix.
 *
 * <p>Only the {@code NODE_COORD_SECTION} formats {@code EUC_2D} (rounded Euclidean distance)
 * and {@code GEO} (geographical distance in kilometres) are supported; both follow the
 * rounding rules of the TSPLIB specification so that tour lengths match the published
 * optima. Files with any other {@code EDGE_WEIGHT_TYPE} (e.g. {@code EXPLICIT},
 * {@code ATT}, {@code CEIL_2D}) are rejected with a {@link TSPException}.
 */
public class TSPFileParser {
    private int graph[][];

    /**
     * @param fileName path to the {@code .tsp} file
     * @throws TSPException if the file is missing, unreadable or in an unsupported format
     */
    public TSPFileParser(String fileName) throws TSPException {
        this.graph = parseFile(fileName);
    }

    /** @return the symmetric distance matrix, indexed by 0-based city number */
    public int[][] getGraph() {
        return this.graph;
    }

    /** Converts a TSPLIB {@code DDD.MM} coordinate (degrees and minutes) to radians. */
    private static double toRadians(double coordinate) {
        int deg = (int) coordinate;
        double min = coordinate - deg;
        return Math.PI * (deg + 5.0 * min / 3.0) / 180.0;
    }

    private static int[][] parseFile(String fileName) throws TSPException {
        // Supported file types
        int EUC_2D = 1;
        int GEO = 2;

        Vector<TSPCoordinate> coords = new Vector<>();
        int fileType = 0;

        try (BufferedReader in = new BufferedReader(new FileReader(fileName))) {
            String line;
            boolean nodeCoordSection = false;

            while ((line = in.readLine()) != null) {
                if (!line.equalsIgnoreCase("EOF") && !line.equalsIgnoreCase(" EOF") && !line.equals("")) {
                    if (!line.equalsIgnoreCase("NODE_COORD_SECTION") && !nodeCoordSection) {
                        if (line.equalsIgnoreCase("EDGE_WEIGHT_TYPE: EUC_2D") || line.equalsIgnoreCase("EDGE_WEIGHT_TYPE : EUC_2D"))
                            fileType = EUC_2D;
                        else if (line.equalsIgnoreCase("EDGE_WEIGHT_TYPE: GEO") || line.equalsIgnoreCase("EDGE_WEIGHT_TYPE : GEO"))
                            fileType = GEO;
                        else if (line.toUpperCase().startsWith("EDGE_WEIGHT_TYPE"))
                            throw new TSPException("Unsupported edge weight type in '" + fileName
                                    + "' (" + line.trim() + "); only EUC_2D and GEO are supported");
                    } else if (line.equalsIgnoreCase("NODE_COORD_SECTION")) {
                        nodeCoordSection = true;
                    } else { // All the numbers are in this part

                        StringTokenizer strTok = new StringTokenizer(line, " \t");
                        try {

                            strTok.nextToken(); // Discard the city number
                            if (fileType == EUC_2D || fileType == GEO) {
                                double x = Double.valueOf(strTok.nextToken());
                                double y = Double.valueOf(strTok.nextToken());
                                coords.addElement(new TSPCoordinate(x, y));
                            } else
                                throw new TSPException("Unrecognized file format!");
                        } catch (NoSuchElementException e) {
                            throw new TSPException("Could not parse file " + "'" + fileName + "'!");
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new TSPException("File " + "'" + fileName + "'" + " not found in the current directory!");
        } catch (IOException e) {
            throw new TSPException("Could not read from file " + "'" + fileName + "'!");
        }

        if (coords.isEmpty())
            throw new TSPException("No NODE_COORD_SECTION found in '" + fileName + "'");

        int graph[][] = new int[coords.size()][coords.size()];

        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j <= i; j++) {
                if (i == j)
                    graph[i][j] = 0;
                else {
                    if (fileType == EUC_2D) {
                        double dX = (coords.elementAt(i)).getX() - (coords.elementAt(j)).getX();
                        double dY = (coords.elementAt(i)).getY() - (coords.elementAt(j)).getY();
                        graph[i][j] = (int) Math.round(Math.sqrt(dX * dX + dY * dY));
                    } else if (fileType == GEO) {
                        // TSPLIB GEO rule: degrees are the integer part (truncated towards
                        // zero, which matters for negative coordinates), minutes the rest.
                        double latitudeI = toRadians((coords.elementAt(i)).getX());
                        double longitudeI = toRadians((coords.elementAt(i)).getY());
                        double latitudeJ = toRadians((coords.elementAt(j)).getX());
                        double longitudeJ = toRadians((coords.elementAt(j)).getY());

                        double RRR = 6378.388;
                        double q1 = Math.cos(longitudeI - longitudeJ);
                        double q2 = Math.cos(latitudeI - latitudeJ);
                        double q3 = Math.cos(latitudeI + latitudeJ);
                        graph[i][j] = (int) (RRR * Math.acos(0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3)) + 1.0);
                    } else
                        throw new TSPException("Unrecognized file format!");
                    graph[j][i] = graph[i][j];
                }

            }
        }

        return graph;
    }
}
