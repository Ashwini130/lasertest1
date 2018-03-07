package com.example.bgcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Looper;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.bgcamera.config.CameraResolution;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;


@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private CameraCallbacks mCameraCallbacks;
    public static String coord="0x0";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Size mPreviewSize;
    private Point[] mPreviewPoints;
    public long h,w;
    private CameraConfig mCameraConfig;
    private WindowManager mWindowManager;
    private volatile boolean safeToTakePicture = false;
    public static int x,y;
    CameraPreview(@NonNull Context context, CameraCallbacks cameraCallbacks) {
        super(context);

        mCameraCallbacks = cameraCallbacks;

        //Set surface holder
        initSurfaceView();
    }

    /**
     * Initilize the surface view holder.
     */
    private void initSurfaceView() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        //Do nothing
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Do nothing
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mCamera == null) {  //Camera is not initialized yet.
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            return;
        } else if (surfaceHolder.getSurface() == null) { //Surface preview is not initialized yet
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        // Make changes in preview size
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();

        //Sort descending
        Collections.sort(pictureSizes, new PictureSizeComparator());

        //set the camera image size based on config provided
        Camera.Size cameraSize;
        switch (mCameraConfig.getResolution()) {
            case CameraResolution.HIGH_RESOLUTION:
                cameraSize = pictureSizes.get(0);   //Highest res
                break;
            case CameraResolution.MEDIUM_RESOLUTION:
                cameraSize = pictureSizes.get(pictureSizes.size() / 2);     //Resolution at the middle
                break;
            case CameraResolution.LOW_RESOLUTION:
                cameraSize = pictureSizes.get(pictureSizes.size() - 1);       //Lowest res
                break;
            default:
                throw new RuntimeException("Invalid camera resolution.");
        }
        parameters.setPictureSize(cameraSize.width, cameraSize.height);

        requestLayout();

        mCamera.setParameters(parameters);

        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            safeToTakePicture = true;
        } catch (IOException | NullPointerException e) {
            //Cannot start preview
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
        if (mCamera != null) mCamera.stopPreview();
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param cameraConfig camera config builder.
     */
    void startCameraInternal(@NonNull CameraConfig cameraConfig) {
        mCameraConfig = cameraConfig;

        if (safeCameraOpen(mCameraConfig.getFacing())) {
            if (mCamera != null) {
                requestLayout();

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
                }
            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    private Mat detectDocument(Bitmap src1)
    {
        Mat src=new Mat();
        Utils.bitmapToMat(src1,src);
        Mat blurred = src.clone();
        //   Imgproc.medianBlur(src, blurred, 9);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2RGB);
        //Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_BGRA2GRAY);
        Log.d("channels", String.valueOf(src.channels())+String.valueOf(src));

        Imgproc.bilateralFilter(src,blurred,11,17,17,Core.BORDER_DEFAULT);
        Imgproc.cvtColor(blurred, blurred, Imgproc.COLOR_RGB2RGBA);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U),gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
        for (int t = 0; t < thresholdLevel; t++) {
            if (t == 0) {
                Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                // ?
            } else {
                Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY,
                        (src.width() + src.height()) / 200, t);
            }

            Imgproc.findContours(gray, contours, new Mat(),
                    Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {
                MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                double area = Imgproc.contourArea(contour);
                approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(temp, approxCurve,
                        Imgproc.arcLength(temp, true) * 0.02, true);

                if (approxCurve.total() == 4 && area >= maxArea) {
                    double maxCosine = 0;

                    List<Point> curves = approxCurve.toList();
                    for (int j = 2; j < 5; j++) {

                        double cosine = Math.abs(angle(curves.get(j % 4),
                                curves.get(j - 2), curves.get(j - 1)));
                        maxCosine = Math.max(maxCosine, cosine);
                    }

                    if (maxCosine < 0.3) {
                        maxArea = area;
                        maxId = contours.indexOf(contour);
                    }
                }
            }
        }
    }

  if (maxId >= 0) {
        Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0,
                .8), 8);

    }
        Rect rect = Imgproc.boundingRect(contours.get(maxId));
        Imgproc.rectangle(src, rect.tl(), rect.br(), new Scalar(255, 0, 0,.8), 2);
        Log.d("rect", String.valueOf(rect));
        int width = rect.width;
        int height = rect.height;
        Log.d("src", String.valueOf(src));
        Log.d("blurred", String.valueOf(blurred));
        Mat test=blurred.clone();
        blurred.convertTo(test,CvType.CV_32FC2);
        Log.d("test", String.valueOf(test));
        //Mat src_mat = new Mat(4, 4, CvType.CV_32FC2);
        //Mat dst_mat = new Mat(4, 4, CvType.CV_32FC2);

        //src_mat.put(0, 0, rect.tl().x, rect.tl().y, rect.br().x,rect.tl().y, rect.br().x, rect.br().y, rect.tl().x, rect.br().y);
        //dst_mat.put(0, 0, 0.0, 0.0, width-1, 0.0, width-1, height-1, 0.0, height-1);
        Point tr=new Point(rect.br().x,rect.tl().y);
        Point bl=new Point(rect.tl().x,rect.br().y);
        MatOfPoint2f src_mat = new MatOfPoint2f(
                rect.tl(), // tl
                tr, // tr
                rect.br(), // br
                 bl// bl
        );

        MatOfPoint2f dst_mat = new MatOfPoint2f(
                new org.opencv.core.Point(0,0), // awt has a Point class too, so needs canonical name here
                new org.opencv.core.Point(width,0),
                new org.opencv.core.Point(width,height),
                new org.opencv.core.Point(0,height)
        );

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Mat doc=new Mat(height,width,CvType.CV_32FC2);
        Imgproc.warpPerspective(blurred, doc, m, doc.size());
        src_mat.release();
        dst_mat.release();
        m.release();
        blurred.release();
        src.release();
        test.release();
        //return src;
        return doc;
    }


private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
        / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
        + 1e-10);
        }
    /*private Mat detectDocument(Bitmap bitmap)
    {
//        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        Mat rgba = new Mat();
       // Mat orig = new Mat();
        Utils.bitmapToMat(bitmap, rgba);
        Mat tmp = new Mat (bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC3);
        Log.d("channels", String.valueOf(rgba.channels())+String.valueOf(rgba));
        //rgba.copyTo(orig);
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2RGB);
        //Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_BGRA2GRAY);
        Log.d("channels", String.valueOf(rgba.channels())+String.valueOf(rgba));
        //Imgproc.cvtColor(rgba,rgba, Imgproc.COLOR_BGRA2BGR);
        //Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2GRAY);

        Imgproc.bilateralFilter(rgba,tmp,11,17,17,Core.BORDER_DEFAULT);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2RGBA);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGBA2GRAY);

        Imgproc.Canny(tmp,tmp,30,200);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(tmp, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
       // Imgproc.drawContours(rgba,contours,-1,new Scalar(0,255,0),5);
       hierarchy.release();
        for(int i=0;i<contours.size()-1;i++)
            for(int j=0;j<contours.size()-1;j++)
                if(Imgproc.contourArea(contours.get(i))<Imgproc.contourArea(contours.get(j)))
                    Collections.swap(contours,i,j);

        Log.d("cntorder","ordering done "+contours.toString());
       // Imgproc.drawContours(rgba,contours,-1,new Scalar(255,0,0),5);
       // ArrayList<MatOfPoint> contours10 = new ArrayList<>();
        //for(int i=0;i<10;i++)
         // contours10.add(i,contours.get(i));
      Rect rect=new Rect();
        MatOfPoint2f screencnt=null;
        Point[] result = {null, null, null, null};
        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
            MatOfPoint points = new MatOfPoint(approx.toArray());
            long total = points.total();
           // Point[] points=approx.toArray();
            if (total == 4) {
                rect = Imgproc.boundingRect(points);
                screencnt=approx;
                break;
            }
        }
            Log.d("screencnt", String.valueOf(screencnt));
           // screencnt.reshape(4,4);
            Log.d("screencnt", String.valueOf(screencnt));
            ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(screencnt.toArray()));
            Log.d("srcpoints",String.valueOf(srcPoints));
         Imgproc.rectangle(rgba,rect.tl(),rect.br(),new Scalar(0,255,0));
            Log.d("rect",rect.tl().toString()+rect.br().toString());
            Comparator<Point> sumComparator = new Comparator<Point>() {
                @Override
                public int compare(Point lhs, Point rhs) {
                    return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
                }
            };

            Comparator<Point> diffComparator = new Comparator<Point>() {

                @Override
                public int compare(Point lhs, Point rhs) {
                    return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
                }
            };

            // top-left corner = minimal sum
            result[0] = Collections.min(srcPoints, sumComparator);

            // bottom-right corner = maximal sum
            result[2] = Collections.max(srcPoints, sumComparator);

            // top-right corner = minimal diference
            result[1] = Collections.min(srcPoints, diffComparator);

            // bottom-left corner = maximal diference
            result[3] = Collections.max(srcPoints, diffComparator);

        Point tl = result[0];
        Point tr = result[1];
        Point br = result[2];
        Point bl = result[3];
        Log.d("points",String.valueOf(tl)+String.valueOf(br));
        //Imgproc.rectangle(rgba,tl,br,new Scalar(0,255,0));
  /*      double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat src_mat = new Mat(4, 4, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 4, CvType.CV_32FC2);

           // int maxWidth = Double.valueOf(dw).intValue();
            //int maxHeight = Double.valueOf(dh).intValue();
        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw-1, 0.0, dw-1, dh-1, 0.0, dh-1);
        // Imgproc.drawContours(rgba,screencnt,-1,new Scalar(0,255,0),3);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Mat doc=new Mat(maxHeight,maxWidth,CvType.CV_32FC2);
        Imgproc.warpPerspective(rgba, doc, m, doc.size());
        rgba.release();
        src_mat.release();
        dst_mat.release();
        m.release();

        return rgba;
        //Bitmap resultBitmap = Bitmap.createBitmap(doc.cols(), doc.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(doc, resultBitmap);

//        Bitmap resultBitmap = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(tmp, resultBitmap);
//        rgba.release();
//        return resultBitmap;
    }
*/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Bitmap detectLight(Bitmap bitmap, double gaussianBlurValue) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat frame = new Mat();
        List<Mat> mChannels = new ArrayList<Mat>();
        Mat frameH;
        Mat frameV;
        Mat frameS;

        // Convert it to HSV
        //Log.d("")
        Imgproc.cvtColor(rgba, frame, Imgproc.COLOR_RGB2HSV);
        // Split the frame into individual components (separate images for H, S,
        // and V)

        mChannels.clear();
        Core.split(frame, mChannels); // Split channels: 0-H, 1-S, 2-V
        frameH = mChannels.get(0);
        frameS = mChannels.get(1);
        frameV = mChannels.get(2);

        // Apply a threshold to each component
        Imgproc.threshold(frameH, frameH, 160, 180, Imgproc.THRESH_BINARY);
        // Imgproc.threshold(frameS, frameS, 0, 100, Imgproc.THRESH_BINARY);
        Imgproc.threshold(frameV, frameV, 200, 256, Imgproc.THRESH_BINARY);
        // Perform an AND operation
        Core.bitwise_and(frameH, frameV, frame);
        //
        //   Core.bitwise_and(frame,frameS,frame);

        //Mat grayScaleGaussianBlur = new Mat();
        //Imgproc.cvtColor(frame, grayScaleGaussianBlur, Imgproc.COLOR_HSV2BGR,1);
        //Imgproc.cvtColor(grayScaleGaussianBlur, grayScaleGaussianBlur, Imgproc.COLOR_BGR2GRAY,1);
        //Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(gaussianBlurValue, gaussianBlurValue), 0);

        Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(frame);
        Imgproc.circle(rgba, minMaxLocResultBlur.maxLoc, 5, new Scalar(255), 1);

        double x = minMaxLocResultBlur.maxLoc.x;
        double y=minMaxLocResultBlur.maxLoc.y;
        coord=Double.toString(x)+"x"+Double.toString(y);

        Bitmap resultBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultBitmap);
        rgba.release();
        frame.release();
        return resultBitmap;

    }
/*
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
     private Bitmap detectLight(Bitmap bitmap, double gaussianBlurValue) {

        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

       // Mat hsv = new Mat();
       // Mat mask = new Mat();
        Mat grayScaleGaussianBlur = new Mat();
        Imgproc.cvtColor(rgba, grayScaleGaussianBlur, Imgproc.COLOR_BGR2GRAY);
       // Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_BGR2HSV);
        Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(gaussianBlurValue, gaussianBlurValue), 0);
        //Core.inRange(hsv,new Scalar(0,0,50),new Scalar(0,0,255),mask);
        Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(grayScaleGaussianBlur);
        Imgproc.circle(rgba, minMaxLocResultBlur.maxLoc, 5, new Scalar(255), 1);

        x = (int)minMaxLocResultBlur.maxLoc.x;
        y=(int)minMaxLocResultBlur.maxLoc.y;
        coord=Integer.toString(x)+"x"+Integer.toString(y);
            Log.e(coord,"coordinates in service of image"+coord);
            String get1 = CameraPreview.coord;
            int hofimage=bitmap.getHeight();
            int wofimage=bitmap.getWidth();

            String cords[]=coord.split("x");

            /*Display mdisp = mWindowManager.getDefaultDisplay();
            android.graphics.Point mdispSize = new android.graphics.Point();
            mdisp.getSize(mdispSize);
            int maxX = mdispSize.x;
            int maxY = mdispSize.y;
            if(hofimage!=0)
            {
                x=x*maxX/hofimage;
                y=y*maxY/wofimage;
                //callfunction
            }
                Intent intent1=new Intent("intentkey");
            intent1.putExtra("key",x+"x"+y);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent1);
        //Bitmap resultBitmap = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mask, resultBitmap);
        //return resultBitmap;
        Bitmap resultBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultBitmap);
        return resultBitmap;
    }
*/
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            stopPreviewAndFreeCamera();

            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e("CameraPreview", "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    boolean isSafeToTakePictureInternal() {
        return safeToTakePicture;
    }

    void takePictureInternal() {
        safeToTakePicture = false;
        if (mCamera != null) {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] bytes, Camera camera) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Convert byte array to bitmap
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Log.d("bitmapval",String.valueOf(bitmap.describeContents()));

                            Mat rgba = detectDocument(bitmap);
                            Bitmap bmp = null;
                            bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(rgba, bmp);
                            bmp=detectLight(bmp,41);
                            //bitmap = detectDocument(bitmap);
                            //Utils.matToBitmap(rgba, bitmap);
                            Log.d("bitmapval",String.valueOf(bitmap.describeContents()));
                            rgba.release();
                                //Save image to the file.
                              /*  if(HiddenCameraUtils.saveImageFromFile(bitmap,
                                    mCameraConfig.getImageFile(),
                                    mCameraConfig.getImageFormat()))*/
                            if(HiddenCameraUtils.saveImageFromFile(bmp,
                                    mCameraConfig.getImageFile(),
                                    mCameraConfig.getImageFormat())){
                                //Post image file to the main thread
                                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCameraCallbacks.onImageCapture(mCameraConfig.getImageFile());
                                    }
                                });
                            } else {
                                //Post error to the main thread
                                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCameraCallbacks.onCameraError(CameraError.ERROR_IMAGE_WRITE_FAILED);
                                    }
                                });
                            }

                            safeToTakePicture = true;
                            //h=bitmap.getHeight();
                            //w=bitmap.getWidth();
                            mCamera.startPreview();
                        }
                    }).start();
                }
            });
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            safeToTakePicture = true;
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    void stopPreviewAndFreeCamera() {
        safeToTakePicture = false;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}