module org.neracaku.neracaku {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires jbcrypt;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens org.neracaku.neracaku to javafx.fxml, javafx.graphics;
    opens org.neracaku.neracaku.controllers to javafx.fxml;
    opens org.neracaku.neracaku.models to javafx.base;

    exports org.neracaku.neracaku;
}