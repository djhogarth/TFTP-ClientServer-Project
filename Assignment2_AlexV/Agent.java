/**
 * Agent is the class for the producer thread.
 */

import java.util.Random;

class Agent implements Runnable {

    private BoundedBuffer buffer;
    private Food PB, JELLY, BREAD;
    private Random rand;

    private final int NUM_SANDWHICHES = 20;

    public Agent(BoundedBuffer buf) {
        buffer = buf;
        PB = new Food("PB");
        JELLY = new Food("JELLY");
        BREAD = new Food("BREAD");
        rand = new Random();
    }

    public void run()
    {
        int n = 0;
        int temp;

        for(int i = 1; i <= NUM_SANDWHICHES; i++) {

            temp = n;
            n = rand.nextInt(99) % 3 + 1; // Values are 1, 2, 3

            //Used to ensure the Random generator doesn't always use the same number consecutively
            while (n == temp) {
                n = rand.nextInt(99) % 3 + 1; // Values are 1, 2, 3
            }

            synchronized (buffer) {
                switch (n) {
                    case 1:
                        buffer.placeFoodOnTable(PB, JELLY, i);
                        System.out.println("[" + i + "] " + Thread.currentThread().getName() + " placed " + PB + " and " + JELLY + " on the table.");
                        break;
                    case 2:
                        buffer.placeFoodOnTable(JELLY, BREAD, i);
                        System.out.println("[" + i + "] " + Thread.currentThread().getName() + " placed " + JELLY + " and " + BREAD + " on the table.");
                        break;
                    case 3:
                        buffer.placeFoodOnTable(PB, BREAD, i);
                        System.out.println("[" + i + "] " + Thread.currentThread().getName() + " placed " + PB + " and " + BREAD + " on the table.");
                        break;
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
    }
}