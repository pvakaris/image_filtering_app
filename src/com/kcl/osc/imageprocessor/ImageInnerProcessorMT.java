package com.kcl.osc.imageprocessor;

import javafx.scene.paint.Color;

/**
 * Runnable object that applies filter to one row of the image it's working on.
 *
 * @author Vakaris Paulavicius (Student number: K20062023)
 * @version 1.6
 */
public class ImageInnerProcessorMT implements Runnable {

    // Row number to put the result to in the filtered image after the algorithm termination.
    private final int rowNumber;
    // Original 2D pixels array.
    private final Color[][] originalPixels;
    // Filter which to apply to each pixel of the required row.
    private final float[][] filter;
    // Filtered row. This array is retrieved by the parent (ImageProcessorMT) when the algorithm
    // terminates
    private Color[] result;
    // When the algorithm is finished, "finished" will be set to true.
    private boolean finished = false;
    // true, if they greyscale filter has to be applied, false otherwise.
    private final boolean applyGreyscale;
    // Row's index in the original image.
    private final int i;

    /**
     * Constructor of ImageInnerProcessorMT.
     * @param pixels Copy of 2D array of pixels of the original image.
     * @param filter A filter to apply to each Color cell in the row. If greyscale is to be
     *               applied, the filter will be null.
     * @param i Row's index in the original 2D pixel array (pixels).
     * @param rowNumber Number of the row in the filtered image. Used to get the position when
     *                  all the rows will be assembled to make the final filtered image.
     * @param applyGreyscale true if greyscale needs to be applied to all row elements.
     */
    public ImageInnerProcessorMT(Color[][] pixels, float[][] filter, int i, int rowNumber, boolean applyGreyscale) {
        this.rowNumber = rowNumber;
        this.filter = filter;
        originalPixels = pixels;
        this.applyGreyscale = applyGreyscale;
        this.i = i;
    }

    /**
     * Run method that is called by the Thread.
     * Starts executing the algorithm.
     */
    @Override
    public void run() {
        // Apply the correct greyscale/filter effect and save the outcome in the result.
        result = applyGreyscale? applyGreyScale() : applyFilter();
        setFinished();
    }

    /**
     * Apply filter to the required row.
     * @return An array of pixels (a row) after the filter has been applied to each pixel in that row.
     */
    private Color[] applyFilter() {
        // An array which to save all the new pixel values to.
        Color[] finalRow = new Color[originalPixels[0].length - 2];
        for (int j = 1; j < originalPixels.length -1; j++) {

            double red = 0.0;
            double green = 0.0;
            double blue = 0.0;

            // Apply the filter for each pixel (i, j) and it's neighbours (radius ---> filter.length).
            for (int k = -1; k < filter.length - 1; k++) {
                for (int l = -1; l < filter[0].length - 1; l++) {
                    red += originalPixels[i + k][j + l].getRed() * filter[1 + k][1 + l];
                    green += originalPixels[i + k][j + l].getGreen() * filter[1 + k][1 + l];
                    blue += originalPixels[i + k][j + l].getBlue() * filter[1 + k][1 + l];
                }
            }

            red = clampRGB(red);
            green = clampRGB(green);
            blue = clampRGB(blue);
            finalRow[j - 1] = new Color(red,green,blue,1.0);
        }
        return finalRow;
    }

    /**
     * Apply greyscale to the required row.
     * @return An array of pixels (a row) after the greyscale has been applied to each pixel in that row.
     */
    private Color[] applyGreyScale() {
        // An array which to save all the new pixel values to.
        Color[] finalRow = new Color[originalPixels[0].length];

        // Apply greyscale to each pixel (i, j).
        for (int j = 0; j < (originalPixels[0].length); j++) {

            double red = originalPixels[i][j].getRed();
            double green = originalPixels[i][j].getGreen();
            double blue = originalPixels[i][j].getBlue();

            double newRGB = (red + green + blue) / 3;
            newRGB = clampRGB(newRGB);

            Color newPixel = new Color(newRGB, newRGB, newRGB, 1.0);
            finalRow[j] = newPixel;
        }
        return finalRow;
    }

    /**
     * This method ensures that the computations on color values have not
     * strayed outside of the range [0,1].
     * @param RGBValue the value to clamp.
     * @return The clamped value.
     */
    protected static double clampRGB(double RGBValue) {
        if (RGBValue < 0.0) {
            return 0.0;
        } else if (RGBValue > 1.0) {
            return 1.0;
        } else {
            return RGBValue;
        }
    }

    /**
     * Set the process as finished. Called when the main algorithm terminates.
     */
    private void setFinished() {
        finished = true;
    }

    /**
     * Used to check whether the main algorithm has concluded. Used by the main process (ImageProcessorMT).
     * @return value of the field "finished".
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Used to get the row number which to put the result
     * in the filtered image to when the algorithm terminates.
     * @return row number.
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * Used to get the row of pixels with applied filter/greyscale.
     * @return The result of the computation.
     */
    public Color[] getResult() {
        return result;
    }
}
