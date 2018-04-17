package a.androidtestcv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.nio.ByteBuffer;
import java.util.List;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.android.ConvertBitmap;
import boofcv.android.VisualizeImageData;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;

import static boofcv.android.ConvertBitmap.declareStorage;

public class Manager {

    private static volatile Manager instance;

    private Manager() {
    }

    public static Manager getInstance() {
        Manager man = instance;
        if (man == null) {
            synchronized (Manager.class) {
                man = instance;
                if (man == null) {
                    instance = man = new Manager();
                }
            }
        }
        return man;
    }

//    public static Bitmap im2gray(Bitmap bitmap) {
//        // https://boofcv.org/index.php?title=Android_support
//        GrayU8 image = ConvertBitmap.bitmapToGray(bitmap, (GrayU8) null, null);
//        return ConvertBitmap.grayToBitmap(image, bitmap.getConfig());
//    }

    public Bitmap showGradient(Bitmap image) {

        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap result = image.copy(image.getConfig(), true);

        GrayU8 grayImage = new GrayU8(width, height);
        ConvertBitmap.bitmapToGray(image, grayImage, null);
        GrayU8 blurred = new GrayU8(width, height);
        BlurImageOps.gaussian(grayImage, blurred, -1, 5, null);

        // Storage for the gradient
        GrayS16 derivX = new GrayS16(1, 1);
        GrayS16 derivY = new GrayS16(1, 1);

        // computes the image gradient
        ImageGradient<GrayU8, GrayS16> gradient = FactoryDerivative.three(GrayU8.class, GrayS16.class);

        derivX.reshape(width, height);
        derivY.reshape(width, height);

        gradient.process(blurred, derivX, derivY);
//        GrayS16 out = new GrayS16(width, height);
//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//                out.set(i, j, derivX.get(i, j) + derivY.get(i, j));
//            }
//        }
//        VisualizeImageData.colorizeGradient(derivX, derivY, -1, result, null);
        gradient(derivX, derivY, -1, result, null);
        return result;
    }

    public Bitmap showFeaturePoint(Bitmap image) {

        int width = image.getWidth();
        int height = image.getHeight();

        GrayU8 grayImage = new GrayU8(width, height);
        ConvertBitmap.bitmapToGray(image, grayImage, null);

        GrayU8 inputA = ConvertBitmap.bitmapToGray(image, null, GrayU8.class, null);

        InterestPointDetector<GrayU8> detector = FactoryInterestPoint.fastHessian(
                new ConfigFastHessian(10, 2, 100, 2, 9, 3, 4));

        // find interest points in the image
        detector.detect(inputA);
        Bitmap bitmap = image.copy(image.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(Color.BLUE);
//        p.setAlpha(50);
        p.setAntiAlias(true);
//        p.setStyle(Paint.Style.STROKE);
        for (int i = 0, size = detector.getNumberOfFeatures(); i < size; i++) {
            Point2D_F64 pt = detector.getLocation(i);

            if (detector.hasScale()) {
                int radius = (int) (detector.getRadius(i));
                canvas.drawCircle((float) pt.x, (float) pt.y, radius / 2 + 1, p);
            }
        }
        return bitmap;
    }

    public Bitmap canny(Bitmap image) {

        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap result = image.copy(image.getConfig(), true);

        GrayU8 grayImage = new GrayU8(width, height);
        ConvertBitmap.bitmapToGray(image, grayImage, null);

        GrayU8 blurred = new GrayU8(image.getWidth(), image.getHeight());
        BlurImageOps.gaussian(grayImage, blurred, -1, 5, null);

        CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(2, true, true, GrayU8.class, GrayS16.class);

        // make sure it doesn't get too low
//        if (threshold <= 0.03f)
        float threshold = 0.27f;

        canny.process(blurred, threshold / 3.0f, threshold, null);
        List<EdgeContour> contours = canny.getContours();

        VisualizeImageData.drawEdgeContours(contours, 0xFFFFFF, result, null);
        return result;
    }

    public Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static void gradient(GrayS16 derivX, GrayS16 derivY,
                                int maxAbsValue, Bitmap output, byte[] storage) {
        shapeShape(derivX, derivY, output);

        if (storage == null)
            storage = declareStorage(output, null);

        if (maxAbsValue < 0) {
            maxAbsValue = ImageStatistics.maxAbs(derivX);
            maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
        }
        if (maxAbsValue == 0)
            return;

        int indexDst = 0;

        for (int y = 0; y < derivX.height; y++) {
            int indexX = derivX.startIndex + y * derivX.stride;
            int indexY = derivY.startIndex + y * derivY.stride;

            for (int x = 0; x < derivX.width; x++) {
                int valueX = derivX.data[indexX++];
                int valueY = derivY.data[indexY++];

                int value = 0;

                if (valueX > 0) {
                    if (valueY > 0) {
                        value = (valueX + valueY) / 2;
                    } else {
                        value = (valueX - valueY) / 2;
                    }
                } else {
                    if (valueY > 0) {
                        value = (-valueX + valueY) / 2;
                    } else {
                        value = (-valueX - valueY) / 2;
                    }
                }
                value = 255 * value / maxAbsValue;
//                value = 255 * value;
                if (value > 255) value = 255;

                storage[indexDst++] = (byte) value;
                storage[indexDst++] = (byte) value;
                storage[indexDst++] = (byte) value;
                storage[indexDst++] = (byte) 0xFF;
            }
        }
        output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
    }

    private static void shapeShape(ImageBase input0, ImageBase input1, Bitmap output) {
        if (output.getConfig() != Bitmap.Config.ARGB_8888)
            throw new IllegalArgumentException("Only ARGB_8888 is supported");
        if (input0.width != output.getWidth() || input0.height != output.getHeight())
            throw new IllegalArgumentException("Input and output must have the same shape");
        if (input1.width != output.getWidth() || input1.height != output.getHeight())
            throw new IllegalArgumentException("Input and output must have the same shape");
    }

    public Bitmap imAdjust(Bitmap input) {
        GrayU8 gray = ConvertBitmap.bitmapToGray(input, (GrayU8) null, null);
        GrayU8 bw = new GrayU8(gray.width, gray.height);
//        GThresholdImageOps.localBlockOtsu(gray, bw, false, ConfigLength.fixed(21), 0.5, 1.0, true);
        GThresholdImageOps.localSauvola(gray, bw, 15, 0.30f, false);
//        GThresholdImageOps.localSquare(gray, bw, 20,0,true,null,null);
//        GThresholdImageOps.localGaussian(gray, bw, 20,0,true,null,null);
        return ConvertBitmap.grayToBitmap(bw, input.getConfig());
    }

    public Bitmap thinning(Bitmap input) {
        GrayU8 gray = ConvertBitmap.bitmapToGray(input, (GrayU8) null, null);
        GrayU8 bw = gray.clone();
        GThresholdImageOps.localSauvola(gray, bw, 15, 0.30f, false);
//        GThresholdImageOps.localSauvola(gray, bw, ConfigLength.fixed(21), 0.30f, true);
        GrayU8 thinned = BinaryImageOps.thin(bw, -1, null);
        return ConvertBitmap.grayToBitmap(thinned, input.getConfig());
    }

//    public Bitmap imAdjust(Bitmap input) {
//        GrayU8 gray = ConvertBitmap.bitmapToGray(input, (GrayU8) null, null);
//        int width = input.getWidth();
//        int height = input.getHeight();
//
//        byte[] storage = declareStorage(input, null);
//
////        int maxAbsValue = ImageStatistics.maxAbs(gray);
////        double meanValue = ImageStatistics.mean(gray);
//
//        int indexDst = 0;
//        for (int y = 0; y < height; y++) {
//            int index = gray.startIndex + y * gray.stride;
//            for (int x = 0; x < width; x++) {
//                int value = gray.data[index++];
//
//                // value from 0 to 127 and then -128 to -1. max = -1; min = 0;
//                if (value > 0 && value < 128) {
////                    System.out.println("value = " + value + "; (byte) value = " + ((byte) value));
//                    value = 0;
//                } else {
////                    value = 255 * value / maxAbsValue;
//                    if (value > 255) value = 255;
//                    if (value <= -1 && value >= -128) {
//                        value += 128;
//                        value *= 2;
//                        value++;
//                    }
//                }
//                storage[indexDst++] = (byte) value;
//                storage[indexDst++] = (byte) value;
//                storage[indexDst++] = (byte) value;
//                storage[indexDst++] = (byte) 0xFF;
//            }
//        }
//        Bitmap output = Bitmap.createBitmap(width, height, input.getConfig());
//        output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
//        return output;
//    }

//    public int map(int input, int prevMinVal, int prevMaxVal, int minValue, int maxValue) {
//        return (input - prevMinVal) / (prevMaxVal - prevMinVal) * (maxValue - minValue) + minValue;
//    }
}

