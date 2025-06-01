package org.neracaku.neracaku.models;

// import javafx.beans.property.SimpleBooleanProperty;
// import javafx.beans.property.SimpleIntegerProperty;
// import javafx.beans.property.SimpleStringProperty;
// import javafx.beans.property.StringProperty;
// import javafx.beans.property.IntegerProperty;
// import javafx.beans.property.BooleanProperty;

public class Category {
    // Jika menggunakan JavaFX Properties (lebih disarankan untuk TableView binding)
    // private final IntegerProperty categoryId;
    // private final StringProperty name;
    // private final StringProperty type; // "pemasukan" atau "pengeluaran"
    // private final BooleanProperty isDefault;
    // private final IntegerProperty userId; // Untuk kategori custom milik user

    // Atau versi POJO sederhana
    private int categoryId;
    private String name;
    private String type;
    private boolean isDefault;
    private Integer userId; // Bisa null untuk kategori default

    public Category() {
        // this.categoryId = new SimpleIntegerProperty();
        // this.name = new SimpleStringProperty();
        // this.type = new SimpleStringProperty();
        // this.isDefault = new SimpleBooleanProperty();
        // this.userId = new SimpleIntegerProperty();
    }

    // Constructor untuk POJO sederhana
    public Category(int categoryId, String name, String type, boolean isDefault, Integer userId) {
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.isDefault = isDefault;
        this.userId = userId;
    }
    public Category(String name, String type) { // Untuk membuat kategori baru tanpa ID
        this.name = name;
        this.type = type;
        this.isDefault = false; // Defaultnya bukan kategori sistem
        // userId akan di-set dari SessionManager
    }


    // --- Getter dan Setter untuk POJO sederhana ---
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isIsDefault() { return isDefault; } // Getter untuk boolean is...
    public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }


    // --- Jika menggunakan JavaFX Properties ---
    // public int getCategoryId() { return categoryId.get(); }
    // public IntegerProperty categoryIdProperty() { return categoryId; }
    // public void setCategoryId(int categoryId) { this.categoryId.set(categoryId); }

    // public String getName() { return name.get(); }
    // public StringProperty nameProperty() { return name; }
    // public void setName(String name) { this.name.set(name); }

    // public String getType() { return type.get(); }
    // public StringProperty typeProperty() { return type; }
    // public void setType(String type) { this.type.set(type); }

    // public boolean isIsDefault() { return isDefault.get(); }
    // public BooleanProperty isDefaultProperty() { return isDefault; }
    // public void setIsDefault(boolean isDefault) { this.isDefault.set(isDefault); }

    // public int getUserId() { return userId.get(); }
    // public IntegerProperty userIdProperty() { return userId; }
    // public void setUserId(int userId) { this.userId.set(userId); }


    @Override
    public String toString() { // Berguna untuk ComboBox atau ListView jika hanya menampilkan nama
        return name;
    }
}