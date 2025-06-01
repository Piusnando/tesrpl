package org.neracaku.neracaku;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.neracaku.neracaku.dao.DatabaseHelper;
import org.neracaku.neracaku.services.AuthService;

import java.io.IOException;
import java.net.URL;

public class  MainApp extends Application {

    private AuthService authService; // Instance AuthService

    @Override
    public void init() throws Exception {
        super.init();
        // Panggil createTablesIfNotExists() dari DatabaseHelper
        // Ini memastikan database dan tabel ada sebelum service apa pun mencoba mengaksesnya.
        DatabaseHelper.createTablesIfNotExists();

        // Inisialisasi AuthService
        authService = new AuthService();
        // Panggil initializeAdminUser dari AuthService
        // Ini akan membuat admin default jika belum ada.
        authService.initializeAdminUser();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Tentukan path ke LoginView.fxml
            // Pastikan path ini benar sesuai dengan struktur folder resources Anda
            // Jika LoginView.fxml ada di org/neracaku/neracaku/fxml/LoginView.fxml
            // maka path-nya adalah "/org/neracaku/neracaku/fxml/LoginView.fxml"
            URL fxmlUrl = getClass().getResource("/org/neracaku/neracaku/fxml/LoginView.fxml");
            if (fxmlUrl == null) {
                System.err.println("Tidak dapat menemukan LoginView.fxml. Periksa path!");
                // Anda mungkin ingin menampilkan dialog error atau keluar dari aplikasi
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root); // Anda bisa menentukan ukuran scene jika mau, misal new Scene(root, 400, 300)

            primaryStage.setTitle("Neracaku - Login");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Gagal memuat LoginView.fxml: " + e.getMessage());
            e.printStackTrace();
            // Tampilkan dialog error ke pengguna atau keluar
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}