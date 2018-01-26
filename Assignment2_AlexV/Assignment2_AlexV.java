/*
  FILENAME -
  ASSIGNMENT - Assignment 1 - SYSC 3303
  AUTHOR - Alex Viman (100967379)
  DETAILS - A program that will generate five RRQ, five WRQ, and one ERROR datagram packets and sends them to IntHost.java
*/

class Assignment2_AlexV {

    public static void main(String args[]) throws Exception {

        System.out.println("Main is running.");
        System.out.println("---- Table Is Empty ----");

        Thread chef1, chef2, chef3, agent;
        BoundedBuffer buffer = new BoundedBuffer();
        Food PB = new Food("PB");
        Food JELLY = new Food("JELLY");
        Food BREAD = new Food("BREAD");

        chef1 = new Thread(new Chef(buffer, PB),"Chef Skippy");
        chef2 = new Thread(new Chef(buffer, JELLY),"Chef Joe");
        chef3 = new Thread(new Chef(buffer, BREAD),"Chef Boyardee");
        agent = new Thread(new Agent(buffer),"Agent");

        chef1.start();
        chef2.start();
        chef3.start();
        agent.start();
    }

}
