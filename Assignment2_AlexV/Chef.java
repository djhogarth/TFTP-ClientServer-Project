/**
 * Consumer is the class for the consumer thread.
 */
class Chef implements Runnable
{
    private BoundedBuffer buffer;
    private Food food;

    public Chef(BoundedBuffer buf, Food f)
    {
        buffer = buf;
        food = f;
    }

    public void run() {

        while (buffer.needMoreSandwiches()) {
            if (buffer.checkTable(food)) {  //Returns true if the table has the right food for this chef
                buffer.makeSammich(food, Thread.currentThread().getName());
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}