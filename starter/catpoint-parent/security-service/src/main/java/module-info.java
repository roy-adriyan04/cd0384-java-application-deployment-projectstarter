module security.service {
    requires image.service;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires com.miglayout.swing;
    requires org.slf4j;
    opens com.udacity.catpoint.data to com.google.gson;
}