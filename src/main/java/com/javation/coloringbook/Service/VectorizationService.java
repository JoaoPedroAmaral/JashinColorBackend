package com.javation.coloringbook.Service;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Service
public class VectorizationService {

    private static final double SIMPLIFICATION_EPSILON = 0.5;

    @Data
    @AllArgsConstructor
    public static class SimplifiedContour {
        private List<Point2D_I32> external;
        private List<List<Point2D_I32>> internal;
    }

    /**
     * Extracts and simplifies contours to maintain original line thickness via filled areas.
     */
    public List<SimplifiedContour> getSimplifiedContours(BufferedImage image) {
        GrayU8 input = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
        GrayU8 binary = new GrayU8(input.width, input.height);

        // 1. Thresholding (dark lines = 1)
        ThresholdImageOps.threshold(input, binary, 120, true);

        // 2. Extract boundaries (outlines) of the black areas
        List<Contour> boofContours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
        
        List<SimplifiedContour> result = new ArrayList<>();
        for (Contour contour : boofContours) {
            // Simplify external boundary
            List<Point2D_I32> extSimplified = simplify(contour.external, SIMPLIFICATION_EPSILON);
            if (extSimplified.size() < 3) continue;

            // Simplify internal holes
            List<List<Point2D_I32>> intSimplified = new ArrayList<>();
            for (List<Point2D_I32> hole : contour.internal) {
                List<Point2D_I32> hSimp = simplify(hole, SIMPLIFICATION_EPSILON);
                if (hSimp.size() >= 3) {
                    intSimplified.add(hSimp);
                }
            }
            
            result.add(new SimplifiedContour(extSimplified, intSimplified));
        }
        return result;
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
