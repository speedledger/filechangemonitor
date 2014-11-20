File Change Monitor
===================

This project consist of only one class that monitors files on disk for changes. Java 8 is required, no other dependencies.
By default java.util.logging is used, but you can plug in your own logging framework if you want.

##Usage example:

    private void fileChanged(String dir, String file) {
        System.out.println("File changed");
    }

    new Thread(new FileChangeMonitor("/directory/test", "README.md", this::fileChanged, 1)).start();
