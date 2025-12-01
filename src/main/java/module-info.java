module com.matveevap.qrapp {
   // requires javafx.controls;
   // requires javafx.fxml;
    requires java.desktop;


    opens com.matveevap.qrapp to javafx.fxml;
    exports com.matveevap.qrapp;
}