package com.kcl.osc.imageprocessor;

/**
 * This class implements a task pool that is responsible for executing tasks.
 * User provides tasks to the task pool, task pool executes those tasks simultaneously at its best available tempo.
 *
 * @author Vakaris Paulavicius (Student number: K20062023)
 * @version 1.7
 */
public class TaskPool implements Runnable{

    // A maximum amount of threads the TaskPool can run simultaneously.
    private final int size;
    // A queue where all the pending tasks are stored.
    private final TaskQueue waitingList;
    // An array tasks that are currently being executed.
    // It is used for continuous checking whether tasks are finished so that
    // their result could be retrieved and new tasks started in their place.
    private final ImageProcessorMT[] tasksRunning;
    // Used to follow the number of tasks that are currently being executed.
    private int numberOfTasksRunning;
    // Is the pool not working.
    private boolean isShutdown = false;

    /**
     * Constructor of the TaskPool.
     * @param size A maximum amount of threads that the pool can take care of at any given time.
     */
    public TaskPool(int size) {
        this.size = size;
        numberOfTasksRunning = 0;
        tasksRunning = new ImageProcessorMT[size];
        waitingList = new TaskQueue();
    }

    /**
     * The main method that is going to be run by the Thread.
     */
    @Override
    public void run() {
        // While the TaskPool is not shutdown, continue to look for finished tasks
        // and if there are any tasks waiting in the queue and there are available space for their execution,
        // start their execution.
        while(!isShutdown) {
            checkForFinishedTasks();
            if(!waitingList.isEmpty()) {
                if(numberOfTasksRunning < size) {
                    // There are tasks waiting in the queue and there is space for execution of new tasks.
                    // Retrieve a new task from the waiting list.
                    ImageProcessorMT task = waitingList.getTask();
                    if(task != null) {
                        startExecuting(task);
                    }
                }
                else {
                    // There are tasks in the waiting line.
                    // However, there is no space for the execution of new tasks at the moment.
                    // Wait for some currently being executed tasks to terminate.
                }
            }
            else {
                // There are no tasks in the waiting list at the moment. Wait for new tasks.
            }
        }
    }

    /**
     * Used to submit a new task to the TaskPool.
     * This task is then put into the TaskQueue.
     * @param task A new task to submit.
     */
    public void submit(ImageProcessorMT task) {
        waitingList.addTask(task);
    }

    /**
     * Used to terminate the task pool.
     */
    public void quit() {
        isShutdown = true;
        System.out.println("Pool was shutdown.");
    }

    //       ==========   PRIVATE METHODS   ==========

    /**
     * Used to start executing a new task.
     * This method is invoked by the run() method when there are tasks in the waiting list
     * and a place for the execution of a new task is found.
     * @param task Task which to start executing.
     */
    private void startExecuting(ImageProcessorMT task) {
        for(int i = 0; i < tasksRunning.length; i ++) {
            // Find an unoccupied place in the array of currently running tasks.
            // All the taken spots will have an object of type ImageProcessorMT stored in them.
            // Free spots will be null.
            if(tasksRunning[i] == null) {
                tasksRunning[i] = task;
                numberOfTasksRunning ++;
                startThread(task);
                // Once a task has been given a space in the array of currently running tasks, return.
                return;
            }
        }
    }

    /**
     * Used to start the thread which will be executing the task.
     * @param task A task that is to be executed.
     */
    private void startThread(ImageProcessorMT task) {
        Thread thread = new Thread(task);
        thread.start();
    }

    /**
     * Used to loop through the array of the currently running tasks to check whether any of them
     * are concluded. Each spot in the array "tasksRunning" that holds a concluded task is vacated.
     */
    private void checkForFinishedTasks() {
        for(int i = 0; i < tasksRunning.length; i ++) {
            if(tasksRunning[i] != null) {
                if(tasksRunning[i].isFinished()) {
                    // Do something with the result of the finished task.
                    tasksRunning[i] = null;
                    numberOfTasksRunning --;
                }
            }
        }
    }
}
