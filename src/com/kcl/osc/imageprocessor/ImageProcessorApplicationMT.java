package com.kcl.osc.imageprocessor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.File;
import java.util.*;

/**
 * This class retrieves images from the 'img' directory located in the main project directory.
 * It then applies the specified filter to all the images.
 *
 * @author unknown. Modified by Vakaris Paulavicius (Student number: K20062023)
 * @version 1.4
 */
public class ImageProcessorApplicationMT extends Application {
	
	/**
	 * Change this constant to change the filtering operation. Options are
	 * IDENTITY, EDGE, BLUR, SHARPEN, EMBOSS, EDGE, GREY
	 */
	private static final String filter = "EMBOSS";
	
	/**
	 * Set this boolean to false if you do NOT wish the new images to be 
	 * saved after processing. 
	 */
	private static final boolean saveNewImages = true;

	// Maximum size of the TaskPool.
	private static final int TASK_POOL_SIZE = 10;
	// TaskPool responsible for applying the filter to images.
	private final TaskPool taskPool = new TaskPool(TASK_POOL_SIZE);

	/**
	 * Method called at the start of the application.
	 * @param stage Stage
	 * @throws Exception Exception
	 */
	@Override
    public void start(Stage stage) throws Exception{
		// Gets the images from the 'img' folder.
		ArrayList<ImageInfo> images = findImages();

		// Put pool into a thread.
		Thread poolThread = new Thread(taskPool);
		// Submit all tasks to the pool
		for (ImageInfo image : images) {
			taskPool.submit(new ImageProcessorMT(image.getImage(), filter, saveNewImages, image.getFilename() + "_filtered.png"));
		}
		// Start a thread that runs the pool.
		poolThread.start();

		// ERROR HERE WHICH I DON'T KNOW HOW TO SOLVE
		// If I start the thread that runs the TaskPool before submitting all the images,
		// it sometimes works (everytime the code is opened from 0), but most of the time does nothing and then terminates.
		// I wrote a test application which has the exact structure but instead of modifying the images,
		// it modifies arrays. There it works just fine. I checked the syntax, it is identical (the logic).
		// Could I get some explanation on what is wrong here? Commented out code below does not work properly.
		// ------------------
		// Thread poolThread = new Thread(taskPool);
		// poolThread.start();
		//
		// for (ImageInfo image : images) {
		// 	   taskPool.submit(new ImageProcessorMT(image.getImage(), filter, saveNewImages, image.getFilename() + "_filtered.png"));
		// }


		// This block provides a lifetime to the TaskPool so that it does not run forever.
		// Wait x seconds for all the tasks to be finished and then shutdown the pool.
		// Task pool could be terminated in the UI, if we had one. For now, we set a lifetime for it.
		{
			int x = 6;
			Thread.sleep(x*1000);
			taskPool.quit();
		}

    	System.out.println("Done.");
    	// Kill this application
		Platform.exit();
    }

	/**
	 * This method expects all the images that are to be processed to
	 * be in a folder called img that is in the current working directory.
	 * In Eclipse, for example, this means the img folder should be in the project
	 * folder (alongside src and bin).
	 * @return Info about the images found in the folder.
	 */
	private ArrayList<ImageInfo> findImages() {
		ArrayList<ImageInfo> images = new ArrayList<>();
		Collection<File> files = listFileTree(new File("img"));
		for (File f: files) {
			if (f.getName().startsWith(".")) {
				continue;
			}
			Image img = new Image("file:" + f.getPath());
			ImageInfo info = new ImageInfo(img, f.getName());
			images.add(info);
		}
		return images;
	}

	/**
	 * Used to get the file tree.
	 * @param dir The directory to look in.
	 * @return A collection of files.
	 */
	private static Collection<File> listFileTree(File dir) {
		Set<File> fileTree = new HashSet<>();
		if (dir.listFiles() == null)
			return fileTree;
		for (File entry : Objects.requireNonNull(dir.listFiles())) {
			if(entry != null) {
				if (entry.isFile())
					fileTree.add(entry) /* */;
				else
					fileTree.addAll(listFileTree(entry));
			}
		}
		return fileTree;
	}

	/**
	 * Start the application.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
        launch(args);
    }
	
	/**
	 * Simply class to hold an Image and its filename.
	 * @author iankenny
	 */
	private static class ImageInfo {
		private final Image image;
		private final String filename;
		
		public ImageInfo(Image image, String filename) {
			this.image = image;
			this.filename = filename;
		}

		public Image getImage() {
			return image;
		}

		public String getFilename() {
			return filename;
		}
	}
}
