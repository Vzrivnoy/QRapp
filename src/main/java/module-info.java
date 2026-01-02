module com.matveevap.qrapp {
//    requires javafx.controls;
//    requires javafx.fxml;
    requires java.desktop;


    opens com.matveevap.qrapp to javafx.fxml;
    exports com.matveevap.qrapp;
    exports com.matveevap.qrapp.enums;
    opens com.matveevap.qrapp.enums to javafx.fxml;
    exports com.matveevap.qrapp.records;
    opens com.matveevap.qrapp.records to javafx.fxml;
}