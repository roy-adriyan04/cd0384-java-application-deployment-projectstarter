module security.service {
    opens com.udacity.catpoint.data;
    requires image.service;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires org.slf4j;
    exports com.udacity.catpoint.application;
    exports com.udacity.catpoint.data;
    exports com.udacity.catpoint.service;
}