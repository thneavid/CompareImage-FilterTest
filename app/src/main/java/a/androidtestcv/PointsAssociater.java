package a.androidtestcv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.android.ConvertBitmap;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

/**
 * After interest points have been detected in two images the next step is to associate the two
 * sets of images so that the relationship can be found.  This is done by computing descriptors for
 * each detected feature and associating them together.  In the code below abstracted interfaces are
 * used to allow different algorithms to be easily used.  The cost of this abstraction is that detector/descriptor
 * specific information is thrown away, potentially slowing down or degrading performance.
 *
 * @author Peter Abeles
 */
public class PointsAssociater<T extends ImageGray<T>, TD extends TupleDesc> {

    // algorithm used to detect and describe interest points
    private DetectDescribePoint<T, TD> detDesc;
    // Associated descriptions together by minimizing an error metric
    private AssociateDescription<TD> associate;

    // location of interest points
    private List<Point2D_F64> pointsA;
    private List<Point2D_F64> pointsB;

    private Class<T> imageType;

    public PointsAssociater(DetectDescribePoint<T, TD> detDesc,
                            AssociateDescription<TD> associate,
                            Class<T> imageType) {
        this.detDesc = detDesc;
        this.associate = associate;
        this.imageType = imageType;
    }

    /**
     * Detect and associate point features in the two images.  Display the results.
     */
    public Bitmap[] associate(Bitmap imageA, Bitmap imageB) {

        int widthA = imageA.getWidth();
        int heightA = imageA.getHeight();

        int widthB = imageB.getWidth();
        int heightB = imageB.getHeight();

        GrayF32 inputA = new GrayF32(widthA, heightA);
        GrayF32 inputB = new GrayF32(widthB, heightB);

        ConvertBitmap.bitmapToGray(imageA, inputA, null);
        ConvertBitmap.bitmapToGray(imageB, inputB, null);

        // stores the location of detected interest points
        pointsA = new ArrayList<>();
        pointsB = new ArrayList<>();

        // stores the description of detected interest points
        FastQueue<TD> descA = UtilFeature.createQueue(detDesc, 100);
        FastQueue<TD> descB = UtilFeature.createQueue(detDesc, 100);

        // describe each image using interest points
        describeImage((T) inputA, pointsA, descA);
        describeImage((T) inputB, pointsB, descB);

        // Associate features between the two images
        associate.setSource(descA);
        associate.setDestination(descB);
        associate.associate();
        FastQueue<AssociatedIndex> matches = associate.getMatches();

        //        Bitmap bitmap = image.copy(image.getConfig(), true);
        Bitmap bitmapA = imageA.copy(imageA.getConfig(), true);
        Bitmap bitmapB = imageB.copy(imageB.getConfig(), true);

        Canvas canvasA = new Canvas(bitmapA);
        Canvas canvasB = new Canvas(bitmapB);
        Paint p = new Paint();
        p.setColor(Color.BLUE);
//        p.setAlpha(50);
        p.setAntiAlias(true);
//        p.setStyle(Paint.Style.STROKE);

        // 4 for 640 x 480 - OK, BUT for 1000 x 1000 - BAD
        // 640 * 480 = 307 200 (4) vs 1000 000 (12 ?)
        float radiusA = 4 * (widthA + heightA) * (widthA + heightA) / (widthA * widthA + heightA * heightA);
        float radiusB = 4 * (widthB + heightB) * (widthB + heightB) / (widthB * widthB + heightB * heightB);
        int best = (int) (radiusA + radiusB);
        for (int i = 0, size = associate.getMatches().getSize() / best; i < size; i++) {
            AssociatedIndex match = matches.get(i);
            p.setColor(Color.BLUE + i + i*i + i*i*i - 1000);

            canvasA.drawCircle(
                    (float) pointsA.get(match.src).getX(),
                    (float) pointsA.get(match.src).getY(),
                    radiusA, p);

            canvasB.drawCircle(
                    (float) pointsB.get(match.dst).getX(),
                    (float) pointsB.get(match.dst).getY(),
                    radiusB, p);

        }
        Bitmap[] bmArray = new Bitmap[2];
        bmArray[0] = bitmapA;
        bmArray[1] = bitmapB;
        return bmArray;
    }

    /**
     * Detects features inside the two images and computes descriptions at those points.
     */
    private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs) {
        detDesc.detect(input);

        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            points.add(detDesc.getLocation(i).copy());
            descs.grow().setTo(detDesc.getDescription(i));
        }
    }

    public static Bitmap[] associateImages(Bitmap imageA, Bitmap imageB) {

        Class imageType = GrayF32.class;
//		Class imageType = GrayU8.class;

        // select which algorithms to use
        DetectDescribePoint detDesc = FactoryDetectDescribe.
                surfStable(new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, imageType);
//				sift(new ConfigCompleteSift(0,5,600));

        ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

        // load and match images
        PointsAssociater app = new PointsAssociater(detDesc, associate, imageType);
        Bitmap[] bmArray = app.associate(imageA, imageB);
        return bmArray;
    }
}