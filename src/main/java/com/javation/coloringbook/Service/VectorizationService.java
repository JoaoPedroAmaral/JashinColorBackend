package com.javation.coloringbook.Service;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Service
public class VectorizationService {

    private static final double SIMPLIFICATION_EPSILON = 0.5;

    /**
     * Uses BoofCV to extract high-quality skeletonized centerlines from a binary image.
     */
    public List<List<Point2D_I32>> getContours(BufferedImage image) {
        GrayU8 input = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
        GrayU8 binary = new GrayU8(input.width, input.height);

        // 1. Thresholding to isolate the lines
        // Lines become 1 (foreground)
        ThresholdImageOps.threshold(input, binary, 128, true);

        // 2. Thinning (Skeletonization)
        // Reduces lines to 1-pixel wide skeletons
        GrayU8 skeleton = new GrayU8(binary.width, binary.height);
        BinaryImageOps.thin(binary, -1, skeleton);

        // 3. Centerline Tracing
        return traceSkeleton(skeleton);
    }

    private List<List<Point2D_I32>> traceSkeleton(GrayU8 skeleton) {
        List<List<Point2D_I32>> paths = new ArrayList<>();
        int w = skeleton.width;
        int h = skeleton.height;
        boolean[][] visited = new boolean[w][h];

        // First pass: Start tracing from endpoints (pixels with only 1 neighbor)
        // this helps capturing linear segments accurately.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (skeleton.get(x, y) == 1 && !visited[x][y]) {
                    if (countNeighbors(skeleton, x, y) == 1) {
                        List<Point2D_I32> path = new ArrayList<>();
                        trace(skeleton, x, y, visited, path);
                        if (path.size() >= 2) {
                            paths.add(simplify(path, SIMPLIFICATION_EPSILON));
                        }
                    }
                }
            }
        }

        // Second pass: Start tracing from any remaining foreground pixels (e.g., closed loops)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (skeleton.get(x, y) == 1 && !visited[x][y]) {
                    List<Point2D_I32> path = new ArrayList<>();
                    trace(skeleton, x, y, visited, path);
                    if (path.size() >= 2) {
                        paths.add(simplify(path, SIMPLIFICATION_EPSILON));
                    }
                }
            }
        }

        return paths;
    }

    private void trace(GrayU8 skeleton, int x, int y, boolean[][] visited, List<Point2D_I32> path) {
        visited[x][y] = true;
        path.add(new Point2D_I32(x, y));

        // Recursive tracing - find an unvisited neighbor and move there
        int[] next = findNextNeighbor(skeleton, x, y, visited);
        if (next != null) {
            trace(skeleton, next[0], next[1], visited, path);
        }
    }

    private int countNeighbors(GrayU8 skeleton, int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < skeleton.width && ny >= 0 && ny < skeleton.height) {
                    if (skeleton.get(nx, ny) == 1) count++;
                }
            }
        }
        return count;
    }

    private int[] findNextNeighbor(GrayU8 skeleton, int x, int y, boolean[][] visited) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < skeleton.width && ny >= 0 && ny < skeleton.height) {
                    if (skeleton.get(nx, ny) == 1 && !visited[nx][ny]) {
                        return new int[]{nx, ny};
                    }
                }
            }
        }
        return null;
    }

    private List<Point2D_I32> simplify(List<Point2D_I32> points, double epsilon) {
        if (points.size() < 3) return points;

        int first = 0;
        int last = points.size() - 1;
        List<Integer> indexToKeep = new ArrayList<>();
        indexToKeep.add(first);
        indexToKeep.add(last);

        douglasPeucker(points, first, last, epsilon, indexToKeep);
        indexToKeep.sort(Integer::compareTo);

        List<Point2D_I32> result = new ArrayList<>();
        for (int index : indexToKeep) {
            result.add(points.get(index));
        }
        return result;
    }

    private void douglasPeucker(List<Point2D_I32> points, int first, int last, double epsilon, List<Integer> indexToKeep) {
        double dmax = 0;
        int index = 0;

        for (int i = first + 1; i < last; i++) {
            double d = perpendicularDistance(points.get(i), points.get(first), points.get(last));
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        if (dmax > epsilon) {
            indexToKeep.add(index);
            douglasPeucker(points, first, index, epsilon, indexToKeep);
            douglasPeucker(points, index, last, epsilon, indexToKeep);
        }
    }

    private double perpendicularDistance(Point2D_I32 p, Point2D_I32 start, Point2D_I32 end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        if (dx == 0 && dy == 0) return Math.hypot(p.x - start.x, p.y - start.y);
        return Math.abs(dy * p.x - dx * p.y + end.x * start.y - end.y * start.x) / Math.hypot(dx, dy);
    }
}
