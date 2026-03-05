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
import java.util.Collections;

@Service
public class VectorizationService {

    private static final double EPSILON = 0.5;

    @Data
    @AllArgsConstructor
    public static class SimplifiedContour {
        private List<Point2D_I32> external;
        private List<List<Point2D_I32>> internal;
    }

    public List<SimplifiedContour> getSimplifiedContours(BufferedImage image) {
        GrayU8 input = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
        GrayU8 binary = new GrayU8(input.width, input.height);
        ThresholdImageOps.threshold(input, binary, 120, true);

        List<Contour> boofContours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
        List<SimplifiedContour> result = new ArrayList<>();

        for (Contour contour : boofContours) {
            List<Point2D_I32> ext = simplify(contour.external, EPSILON);
            if (ext.size() < 3) continue;

            List<List<Point2D_I32>> holes = new ArrayList<>();
            for (List<Point2D_I32> hole : contour.internal) {
                List<Point2D_I32> hSimp = simplify(hole, EPSILON);
                if (hSimp.size() >= 3) holes.add(hSimp);
            }
            result.add(new SimplifiedContour(ext, holes));
        }
        return result;
    }

    private List<Point2D_I32> simplify(List<Point2D_I32> points, double epsilon) {
        if (points.size() < 3) return points;
        List<Integer> kept = new ArrayList<>();
        kept.add(0);
        kept.add(points.size() - 1);
        douglasPeucker(points, 0, points.size() - 1, epsilon, kept);
        Collections.sort(kept);

        List<Point2D_I32> result = new ArrayList<>();
        for (int idx : kept) result.add(points.get(idx));
        return result;
    }

    private void douglasPeucker(List<Point2D_I32> pts, int first, int last, double eps, List<Integer> kept) {
        double dmax = 0;
        int idx = 0;
        for (int i = first + 1; i < last; i++) {
            double d = dist(pts.get(i), pts.get(first), pts.get(last));
            if (d > dmax) {
                idx = i;
                dmax = d;
            }
        }
        if (dmax > eps) {
            kept.add(idx);
            douglasPeucker(pts, first, idx, eps, kept);
            douglasPeucker(pts, idx, last, eps, kept);
        }
    }

    private double dist(Point2D_I32 p, Point2D_I32 start, Point2D_I32 end) {
        double dx = end.x - start.x, dy = end.y - start.y;
        if (dx == 0 && dy == 0) return Math.hypot(p.x - start.x, p.y - start.y);
        return Math.abs(dy * p.x - dx * p.y + end.x * start.y - end.y * start.x) / Math.hypot(dx, dy);
    }
}
