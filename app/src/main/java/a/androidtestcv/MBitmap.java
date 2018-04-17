package a.androidtestcv;

import android.graphics.Bitmap;

public class MBitmap {

    public final int[] dataArray;
    public final int width;
    public final int height;

    public MBitmap(int width, int height) {
        this(new int[width * height], width, height);
    }

    public MBitmap(int[] dataArray, int width, int height) {
        this.dataArray = dataArray;
        this.width = width;
        this.height = height;
    }

    public int get(int x, int y) {
        return dataArray[y * width + x];
    }

    public void set(int x, int y, int value) {
        dataArray[y * width + x] = value;
    }

    public static Bitmap convolute(Bitmap inputBitmap, int[][] kernel, int kernelDivisor) {
        int inputWidth = inputBitmap.getWidth();
        int inputHeight = inputBitmap.getHeight();
        int kernelWidth = kernel[0].length;
        int kernelHeight = kernel.length;
        if ((kernelWidth <= 0) || ((kernelWidth & 1) != 1))
            throw new IllegalArgumentException("Kernel must have odd width");
        if ((kernelHeight <= 0) || ((kernelHeight & 1) != 1))
            throw new IllegalArgumentException("Kernel must have odd height");
        int kernelWidthRadius = kernelWidth >>> 1;
        int kernelHeightRadius = kernelHeight >>> 1;

        Bitmap outputBitmap = Bitmap.createBitmap(inputWidth, inputHeight, inputBitmap.getConfig());
        for (int i = inputWidth - 1; i >= 0; i--) {
            for (int j = inputHeight - 1; j >= 0; j--) {
                double newValue = 0.0;
                for (int kw = kernelWidth - 1; kw >= 0; kw--)
                    for (int kh = kernelHeight - 1; kh >= 0; kh--)
                        newValue += kernel[kw][kh] * inputBitmap.getPixel(
                                bound(i + kw - kernelWidthRadius, inputWidth),
                                bound(j + kh - kernelHeightRadius, inputHeight));
                outputBitmap.setPixel(i, j, (int) Math.round(newValue / kernelDivisor));
            }
        }
        return outputBitmap;
    }

    private static int bound(int value, int endIndex) {
        if (value < 0)
            return 0;
        if (value < endIndex)
            return value;
        return endIndex - 1;
    }
}
