public class BoundedBuffer
{
    private static final int SIZE = 3;
    private Food[] buffer = new Food[SIZE];
    private int sammichCounter = 0;

    private boolean writeable = true;  // If true, there is room for at least one object in the buffer.
    private boolean readable = false;  // If true, there is at least one object stored in the buffer.
    private boolean notMade = true;    // If true, sandwhich has not been made yet

    public synchronized void placeFoodOnTable(Food item1, Food item2, int counter)
    {
        while (!writeable || !notMade) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        sammichCounter = counter;

        buffer[0] = item1;
        buffer[1] = item2;
        readable = true;

        notifyAll();
    }

    public synchronized boolean checkTable(Food chefsFood)
    {
        boolean alreadyThere = false;

        while (!readable || !notMade) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        if (!alreadyThere && notMade) {
            if (buffer[0].foodType.equals(chefsFood.foodType))
                alreadyThere = true;

            if (buffer[1].foodType.equals(chefsFood.foodType))
                alreadyThere = true;
        }

        return  (!alreadyThere);
    }

    public synchronized void makeSammich(Food chefsFood, String chefsName)
    {
        while (!readable) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        buffer[2] = chefsFood;
        writeable = false;
        notMade = false;
        notifyAll();

        System.out.println("[" + sammichCounter + "] " + chefsName + " offered " + chefsFood + ".");
        System.out.println("[" + sammichCounter + "] " + chefsName + " ate the sandwhich.");
        resetVars();
    }

    private void resetVars()
    {
        System.out.println("---- Table Is Empty ----");
        this.buffer = null;
        this.buffer = new Food[SIZE];
        this.writeable = true;
        this.readable = false;
        this.notMade = true;
        notifyAll();

        if (!needMoreSandwiches())
            System.exit(1);
    }

    public boolean needMoreSandwiches()
    {
        if (this.sammichCounter < 20)
            return true;
        else
            return false;
    }
}