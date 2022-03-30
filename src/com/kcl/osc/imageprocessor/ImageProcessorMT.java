package com.kcl.osc.imageprocessor;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.LinkedList;

/**
 * An object of this class is responsible for applying the filter to the image it holds.
 * It converts image to a 2D array of pixels, retrieves the correct filter and then applies the filter
 * to each pixel of the image.
 *
 * It divides the image into equal slices (rows) and for each row starts an ImageInnerProcessorMT which
 * applies the filter to each pixel in that row.
 *
 * After all the rows have been filtered, the object terminates its algorithm and saves the filtered image in the "result"
 * field. Depending on the provided "save" value it either saves the new image with the given "fileName"
 * to the main directory or does not.
 *
 * @author unknown, modified by Vakaris Paulavicius (Student number: K20062023).
 * @version 1.9
 */
public class ImageProcessorMT implements Runnable{

	// The original image which to apply the filter to.
	private final Image image;
	// Name of the filtered saved image in the main directory.
	private final String fileName;
	// Name of the filter which to apply.
	private final String filterType;
	// Whether save the image to the main directory after the algorithm terminates or not.
	private final boolean save;
	// 2D array of pixels after the execution of the algorithm.
	private final Color[][] filteredImage;
	// Whether the algorithm has concluded.
	private boolean finished = false;
	// Maximum number of ImageInnerProcessorMT objects that can apply filter to the rows at the same time.
	private static final int MAX_NUMBER_OF_INNER_PROCESSORS = 32;
	// Number of ImageInnerProcessorMT objects currently applying filter to the rows.
	private int numberOfInnerProcessorsRunning;
	// An array of ImageInnerProcessorMT objects that are applying filter to the rows.
	private final ImageInnerProcessorMT[] innerProcessors;
	// A list of rows in the image.
	private final LinkedList<ImageInnerProcessorMT> slices;

	/**
	 * Constructor.
	 * @param image The image to process.
	 * @param filter The filter to use.
	 * @param save Whether to save the new image or not.
	 * @param fileName The output image filename.
	 */
	public ImageProcessorMT(Image image, String filter, boolean save, String fileName) {
		this.image = image;
		this.fileName = fileName;
		this.filterType = filter;
		this.save = save;
		filteredImage = new Color[(int)image.getHeight()][(int)image.getWidth()];
		innerProcessors = new ImageInnerProcessorMT[MAX_NUMBER_OF_INNER_PROCESSORS];
		slices = new LinkedList<>();
		numberOfInnerProcessorsRunning = 0;
		// Divides the image into rows so that the algorithm can be applied.
		divideTask();
	}

	/**
	 * Used to divide the image into rows that can be later processed simultaneously.
	 */
	private void divideTask() {
		if (filterType.equals("GREY")) {
			divideTaskForGreyscale();
		}
		else {
			divideTaskForCustomFilter();
		}
	}

	/**
	 * Creates the filter.
	 * @param filterType The type of filter required.
	 * @return The filter.
	 */
	private float[][] createFilter(String filterType) {
		filterType = filterType.toUpperCase();

		switch (filterType) {
			case "IDENTITY":
				return (new float[][]{{0, 0, 0}, {0, 1, 0}, {0, 0, 0}});
			case "BLUR":
				return (new float[][]{{0.0625f, 0.125f, 0.0625f}, {0.125f, 0.25f, 0.125f}, {0.0625f, 0.125f, 0.0625f}});
			case "SHARPEN":
				return (new float[][]{{0, -1, 0}, {-1, 5, -1}, {0, -1, 0}});
			case "EDGE":
				return (new float[][]{{-1, -1, -1}, {-1, 8, -1}, {-1, -1, -1}});
			case "EMBOSS":
				return (new float[][]{{-2, -1, 0}, {-1, 0, 1}, {0, 1, 2}});
		}
		return null;
	}

	/**
	 * Divide the image into rows where each row has to apply greyscale filter to its pixels.
	 */
	private void divideTaskForGreyscale() {
		// Get 2D array of pixels.
		Color[][] inputPixels = getPixelData();

		for (int i = 0; i < (inputPixels.length);i++) {
			slices.addLast(new ImageInnerProcessorMT(inputPixels, null, i, i, true));
		}
	}

	/**
	 * Divide the image into rows where each row has to apply filter to its pixels.
	 */
	private void divideTaskForCustomFilter() {
		// Get 2D array of pixels with an additional 1px grey border.
		Color[][] pixels = getPixelDataExtended();
		// Get the required filter according to the provided filter name.
		float[][] filter = createFilter(filterType);

		for (int i = 1; i < pixels.length -1; i++) {
			slices.addLast(new ImageInnerProcessorMT(pixels, filter, i, i-1, false));
		}
	}

	/**
	 * Runs this image processor.
	 */
	@Override
	public void run() {
		// To follow the time that the process takes to execute
		long startTime = System.nanoTime();
		System.out.println("Started applying filter " + filterType.toUpperCase() + " to image " + image + ".");
		while (!finished) {
			checkForFinishedSubtasks();
			// While there are rows that the filter was not applied to yet.
			if(!slices.isEmpty()) {
				// And there is space for new rows to be modified at this time.
				if(numberOfInnerProcessorsRunning < MAX_NUMBER_OF_INNER_PROCESSORS) {
					// Start applying the filter to a new slice.
					startExecutingSlice(slices.removeFirst());
				}
				else {
					// Maximum amount of threads are now simultaneously modifying rows.
					// Wait for at least one of them to finish applying the filter.
				}
			}
			else {
				// No more rows left to modify.
				// Let all the threads finish their work on the remaining
				// slices and then close this task.
			}
			checkIfFinished();
		}
		setFinished();
		long timeTaken = System.nanoTime() - startTime;
		if(save) {
			System.out.println("Finished applying filter to image " + image + ". Image saved as: " + fileName);
		}
		else {
			System.out.println("Finished applying filter to image " + image + ".");
		}
		System.out.println("Time taken: " + timeTaken + "ns.");
	}


	/**
	 * Used to start executing applying the filter to a new slice.
	 * This method is invoked by the run() method when there are unmodified slices and
	 * there is space in the innerProcessors array.
	 * @param slice A new slice to apply the filter to.
	 */
	private void startExecutingSlice(ImageInnerProcessorMT slice) {
		for(int i = 0; i < innerProcessors.length; i ++) {
			// Find an unoccupied place in the array of currently running inner processors.
			// All the taken spots will have an object of type ImageInnerProcessorMT stored in them.
			// Free spots will be null.
			if(innerProcessors[i] == null) {
				innerProcessors[i] = slice;
				numberOfInnerProcessorsRunning ++;
				startThread(slice);
				return;
			}
		}
	}

	/**
	 * Used to start the thread which will be applying filter to the given slice.
	 * @param slice A slice that is to be applied to.
	 */
	private void startThread(ImageInnerProcessorMT slice) {
		Thread thread = new Thread(slice);
		thread.start();
	}

	/**
	 * Check if the image filtering is complete.
	 */
	private void checkIfFinished() {
		// Keeps track of how many lines in the image are still to be modified.
		int missingElements = 0;
		// Checks each row of the filtered image and counts how many rows have not been modified yet.
		// Their pixels will be null.
		for (Color[] colors : filteredImage) {
			if (colors[0] == null) {
				missingElements++;
			}
		}
		// If the filter was applied to all the rows, setFinished().
		if(missingElements == 0) {
			setFinished();
		}
	}

	/**
	 * Continuously checks whether there are any finished tasks in the innerProcessors array.
	 * If there are, their result is put into the final image and their occupied space is freed.
	 */
	private void checkForFinishedSubtasks() {
		for(int i = 0; i < innerProcessors.length; i ++) {
			if(innerProcessors[i] != null) {
				if(innerProcessors[i].isFinished()) {
					// If the filter was applied to the row, retrieve its index in the final image
					// and put the row in that position.
					int resultIndex = innerProcessors[i].getRowNumber();
					filteredImage[resultIndex] = innerProcessors[i].getResult();
					// Free the spot so that new rows could be modified.
					innerProcessors[i] = null;
					numberOfInnerProcessorsRunning --;
				}
			}
		}
	}

	/**
	 * Used to check if the filtering process is finished.
	 * @return if finished.
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * Set the image filtering process as finished and save the image to the main
	 * project directory if it is a requirement.
	 */
	private void setFinished() {
		finished = true;
		if (save) {
			saveNewImage(filteredImage, fileName);
		}
	}

	/**
	 * Saves the pixel data in the parameter as a new image file.
	 * @param pixels The pixel data.
	 * @param filename The output filename.
	 */
	private void saveNewImage(Color[][] pixels, String filename) {
		WritableImage wImg = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());

		PixelWriter pw = wImg.getPixelWriter();
		for (int i = 0; i < wImg.getHeight(); i++) {
			for (int j = 0; j < wImg.getWidth(); j++) {
				pw.setColor(i, j, pixels[i][j]);
			}
		}

		File newFile = new File(filename);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(wImg, null), "png", newFile);
		} catch (Exception s) {
			//
		}
	}


	/**
	 * Gets the pixel data from the image but does
	 * NOT add a border.
	 * @return The pixel data.
	 */
	private Color[][] getPixelData() {
		PixelReader pr = image.getPixelReader();
		Color[][] pixels = new Color[(int) image.getWidth()][(int) image.getHeight()];
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				pixels[i][j] = pr.getColor(i, j);
			}
		}

		return pixels;
	}

	/**
	 * Gets the pixel data from the image but with a one-pixel border added.
	 * @return The pixel data.
	 */
	private Color[][] getPixelDataExtended() {
		PixelReader pr = image.getPixelReader();
		Color[][] pixels = new Color[(int) image.getWidth() + 2][(int) image.getHeight() + 2];

		for (int i = 0; i < pixels.length; i++) {
			for (int j = 0; j < pixels.length; j++) {
				pixels[i][j] = new Color(0.5, 0.5, 0.5, 1.0);
			}
		}

		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				pixels[i + 1][j + 1] = pr.getColor(i, j);
			}
		}

		return pixels;
	}
}