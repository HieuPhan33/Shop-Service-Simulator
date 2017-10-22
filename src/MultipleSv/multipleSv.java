package MultipleSv;
import java.io.*;
import java.util.Scanner;

public class multipleSv {

    static server servers[] = new server[21];
    static int array[] = new int[21];
    static int size = 0;
    static heapEvents eventQueue = new heapEvents();
    static queue customerQueue = new queue(1000);

    private static Scanner inStream;

    public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		System.out.print("Enter your file name : ");
        try {
            inStream = new Scanner(new File(input.next()));
        } catch (IOException e) {
            System.out.println("Failed to open file. Check your file name again");
        }
        //Get total number of servers
        int totalSv = inStream.nextInt();
        //Add each servers to array of servers
        for (int i = 1; i <= totalSv; i++) {
            addSv(inStream.nextDouble());
        }

        //Read first customer arrival from file and add to event queue
        eventQueue.addArrivalEvent(inStream.nextDouble(), inStream.nextDouble());
        queue arrivingTime = new queue(30);
        double time = 0;
        double interval = 0;
        int totalCustomer = 1;
        double totalLength = 0;
        int maxLength = 0;
        double totalWaitingTime = 0;
        //int currentLength = 0;
        //Keep processing until event queue still is empty, customer queue is empty
        // and all servers are idle
        while (!eventQueue.isEmpty()) {
            event newEvent = eventQueue.pop();
            //Interval time between current event and last event
            interval = newEvent.eventTime - time;
            //Jump into latest event time
            time = newEvent.eventTime;

            //total length is incremented by length of queue times how long that length stays over interval time
            totalLength += (customerQueue.length() * interval);
            //If next event is arrival
            if (newEvent.eventType == -1) {
                //Add arrival to the queue
                customerQueue.enqueue(newEvent.serviceTime);
                //If it is not end of file, get next arrival and add new event
                //to event queue
                if (inStream.hasNext()) {
                    totalCustomer++;
                    eventQueue.addArrivalEvent(inStream.nextDouble(), inStream.nextDouble());

                }
            } //If next event is customer completement
            else {
                //Set server which that customer comes out to idle, set finish time = 0
                servers[newEvent.eventType].setIdle(time);
                //heap sort to maintain property of the heap of servers
                heapSort();
            }
            //If some servers are idle and queue still has some customers,
            // we put them to idle server and serve
            while (!allBusy() && !customerQueue.isEmpty()) {
                //Put them to the most efficient available server
                double serviceTime = customerQueue.dequeue();
                double finishTime = time + servers[array[1]].multiplier
                        * serviceTime;
                servers[array[1]].serveCustomer(finishTime, time);
                //heap sort to maintain property of the heap of servers

                //Add customer completement event 
                eventQueue.addCustomerCompletementEvent(array[1], finishTime, serviceTime);
                //Heap sort the heap of servers 
                heapSort();
            }
            // currentLength = customerQueue.length();
            if (customerQueue.length() > maxLength) {
                maxLength = customerQueue.length();
            }
            //If new customer arrives and customer queue is full, we record this arrival time in the queue
            if (newEvent.eventType == -1) {
                if (!customerQueue.isEmpty()) {
                    arrivingTime.enqueue(time);
                }
            }//If one server finished, and queue of arriving time is not empty,
            // total time spent by a customer is 
            //equal to (current time - arriving time of earliest customer waiting in queue)
            else if (!arrivingTime.isEmpty()) {
                totalWaitingTime += (time - arrivingTime.dequeue());
            }

        }
        System.out.println("Time last customer completed service " + time);
        System.out.println("The total customer is " + totalCustomer);
        System.out.println("Greatest length is " + maxLength);
        System.out.println("Average length of the queue is " + (totalLength / time));
        System.out.println("Average time spent on queue by each customer " + (totalWaitingTime / totalCustomer));
        System.out.printf("%10s%11s%18s%12s\n", "Checkout", "Priority", "CustomersServed", "IdleTime");
        for (int i = 1; i <= size; i++) {
            System.out.printf("%4d%10s%1.4f%13d%10s%6.4f%2s\n", i - 1, "", servers[i].multiplier, servers[i].customerServed,
                    "", servers[i].idleTime, "");
        }
    }

    //Add new server
    static void addSv(double multiplier) {
        server newSv = new server(multiplier);
        size++;
        //Assign global server
        servers[size] = newSv;
        servers[size].setIdle(0);
        //Let the index of the server point to it
        array[size] = size;
        //Heap sort to maintain the most efficient server on top
        heapSort();
    }

    static void heapSort() {
        //Rearrange array
        for (int i = size / 2; i >= 1; i--) {
            heapify(i, size);
        }
        //Swap current root with the end to put largest element in right order,
        // and heapify the rest elements to ensure current root is always the largest
        for (int i = size - 1; i >= 1; i--) {
            int temp = array[1];
            array[1] = array[i + 1];
            array[i + 1] = temp;
            heapify(1, i);

        }
    }

    static boolean allIdle() {
        int i = 1;
        //Break if we find any busy server
        while (i <= size && !servers[i].busy) {
            i++;
        }
        return (i == (size + 1));
    }

    static boolean allBusy() {
        int i = 1;
        //Break if we find any idle server
        while (i <= size && servers[i].busy) {
            i++;
        }
        return (i == (size + 1));
    }

    //heapify element i in a heap size of n
    static void heapify(int i, int n) {

        int largest = i;  // Initialize largest as root
        int l = 2 * i;  // left = 2*i 
        int r = 2 * i + 1;  // right = 2*i + 1
        //Find the real largest elment
        if (l <= n && servers[array[largest]].compareTo(servers[array[l]]) < 0) {
            largest = l;
        }
        if (r <= n && servers[array[largest]].compareTo(servers[array[r]]) < 0) {
            largest = r;
        }
        //If largest is not the parent, we swap the largest with the current parent
        // and continue to heapify down
        if (largest != i) {
            int temp = array[i];
            array[i] = array[largest];
            array[largest] = temp;
            heapify(largest, n);
        }

    }
}

class server implements Comparable<server> {

    int customerServed;
    double idleTime;
    double startIdle;
    double multiplier;
    boolean busy;
    double finishTime;

    server(double multiplier) {
        this.multiplier = multiplier;
        busy = false;
    }

    void setIdle(double time) {
        startIdle = time;
        busy = false;
        finishTime = 0;
    }

    void serveCustomer(double finishTime, double time) {
        customerServed++;
        idleTime += (time - startIdle);
        this.finishTime = finishTime;
        busy = true;
    }

    //Make sure the server with earlier finish time comes at the front
    //If finish time is equal, the more efficient one comes at the front
    public int compareTo(server other) {
        if (this.finishTime > other.finishTime) {
            return 1;
        }
        if (this.finishTime < other.finishTime) {
            return -1;
        }
        if (this.multiplier < other.multiplier) {
            return -1;
        }
        if (this.multiplier > other.multiplier) {
            return 1;
        }
        return 0;

    }

}
//Heap of events make sure to put the earliest event on the top, in other words,
// the smallest event is on top according to compareTo method
class heapEvents {

    event array[] = new event[5000];
    int size = 0;

    heapEvents() {
        for (int i = 1; i < 5000; i++) {
            array[i] = null;
        }
    }

    void siftUp(int index) {
        int parent = index / 2;
        //If element at index is larger than element parent or element index is a root
        //, we return
        if (index == 1 || array[parent].compareTo(array[index]) < 0) {
            return;
        }
        //If not, swap element index with element parent
        event temp = array[parent];
        array[parent] = array[index];
        array[index] = temp;

        siftUp(parent);
    }

    void siftDown(int index) {
        //If element index is a leaf, we return
        if (index > size / 2) {
            return;
        }
        int smallChild = index * 2;
        //Find the smallest event
        if (array[smallChild].compareTo(array[index * 2 + 1]) > 0) {
            smallChild++;
        }
        //If the parent is not the smallest, we swap parent with the smallest
        if (array[index].compareTo(array[smallChild]) > 0) {
            event temp = array[index];
            array[index] = array[smallChild];
            array[smallChild] = temp;
        } else {
            return;
        }
        siftDown(smallChild);
    }

    void addArrivalEvent(double eventTime, double serviceTime) {
        event arrivalEvent = new event(-1, eventTime, serviceTime);
        size++;
        array[size] = arrivalEvent;
        //Sift up to put the earliest event on top
        siftUp(size);
    }

    void addCustomerCompletementEvent(int serverNo, double eventTime, double serviceTime) {
        event customerCompletement = new event(serverNo, eventTime, serviceTime);
        size++;
        array[size] = customerCompletement;
        //Sift up to put the earliest event on top
        siftUp(size);

    }

    event pop() {
        event result = null;
        if (size != 0) {
            result = array[1];
            array[1] = array[size];
            size--;
            //Sift down to maintain heap's property
            siftDown(1);
        }
        return result;
    }

    boolean isEmpty() {
        return (size == 0);
    }

}

class event implements Comparable<event> {

    int eventType;
    double eventTime;
    double serviceTime;

    event(int type, double time, double svTime) {
        eventType = type;
        eventTime = time;
        serviceTime = svTime;
    }

    public int compareTo(event t) {
        double result = this.eventTime - t.eventTime;
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        }
        return -(eventType);
    }

}

class queue {

    double value[];
    int capacity;
    int index = 1;
    int start = 1;

    queue(int capacity) {
        this.capacity = capacity;
        value = new double[capacity];
        for (int i = 1; i < capacity; i++) {
            value[i] = -1;
        }
    }

    void enqueue(double value) {
        this.value[index] = value;
        index++;
        if (index == capacity - 1) {
            index = 1;
        }
    }

    double dequeue() {
        double result = 0;
        if (!isEmpty()) {
            result = value[start];
            start++;
            if (start == capacity - 1) {
                start = 1;
            }
        }
        return result;
    }

    boolean isEmpty() {
        return start == index;
    }

    public int length() {
        int length = 0;
        //When index exceeds capacity and index = 1, our queue contains elements
        //from start to the capacity and from 1 to the index
        if (start > index) {
            for (int i = start; i < capacity; i++) {
                if (value[i] > -1) {
                    length++;
                }
            }
            for (int i = 1; i < index; i++) {
                if (value[i] > -1) {
                    length++;
                }
            }
        } else {
            for (int i = start; i < index; i++) {
                if (value[i] > -1) {
                    length++;
                }
            }
        }
        return length;
    }

}
