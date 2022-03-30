package com.kcl.osc.imageprocessor;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class represents a task queue used in the TaskPool. It stores all the pending tasks
 * and lets the user put and remove tasks from it safely, with the help of the  reentrant lock.
 *
 * @author Vakaris Paulavicius (Student number: K20062023)
 * @version 1.2
 */
public class TaskQueue {
    // A lock to prevent multiple threads from modifying the waiting list simultaneously.
    private final ReentrantLock lock = new ReentrantLock(true);
    // A LinkedList that stores all the pending tasks. Works like a queue structure ---> First in, first out.
    private final LinkedList<ImageProcessorMT> waitingList = new LinkedList<>();

    /**
     * Constructor of the TaskQueue.
     */
    public TaskQueue() {
    }

    /**
     * Used to insert a task into the end of the queue.
     * @param task A tasks which to insert to.
     */
    public void addTask(ImageProcessorMT task) {
        // Lock the queue preventing multiple accesses.
        lock.lock();
        try {
            // Add a new task to the end of the queue.
            waitingList.addLast(task);
        }
        finally {
            // Unlock the queue.
            lock.unlock();
        }
    }

    /**
     * Used to remove the first task from the queue and return it.
     * @return Return the first task from the queue, null if the queue is empty.
     */
    public ImageProcessorMT getTask() {
        // Lock the queue preventing multiple accesses.
        lock.lock();
        ImageProcessorMT task = null;
        try {
            // If there are any tasks in the waiting list, remove the first one and return it.
            // Else, return null.
            if(size() > 0) {
               task = waitingList.removeFirst();
            }
        } finally {
            // Unlock the queue.
            lock.unlock();
        }
        return task;
    }

    /**
     * Used to get the amount of tasks in the queue.
     * @return The size of the queue.
     */
    public int size() {
        return waitingList.size();
    }

    /**
     * Used to check whether the queue is empty.
     * @return true if the queue is empty, false otherwise.
     */
    public boolean isEmpty() {
        return size() == 0;
    }


}
