File Change Monitor
===================

This project consist of only one class that monitors files on disk for changes. We use this to monitor properties files to update the state of servers in runtime. 

java.nio.file.WatchService is used for the monitoring. Java 8 is required, no other dependencies. By default java.util.logging is used, but you can plug in your own logging framework if you want.

##Usage example:

public class Example {
    void fileChanged(String dir, String file) {
        System.out.println("File changed");
    }

    public void start() {
        new Thread(new FileChangeMonitor("/home/speedledger", "README.md", this::fileChanged, 1)).start();
    }
    
    public static void main(String[] args) {
        new Example().start();
    }
}
