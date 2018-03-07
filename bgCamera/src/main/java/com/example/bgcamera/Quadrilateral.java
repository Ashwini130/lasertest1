package com.example.bgcamera;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

/**
 * Created by Ashwini on 27-02-2018.
 */

public class Quadrilateral {
    public MatOfPoint contour;
    public Point[] points;

    public Quadrilateral(MatOfPoint contour, Point[] points) {
        this.contour = contour;
        this.points = points;
    }
}
