package org.neracaku.neracaku.utils; // PASTIKAN PACKAGE SESUAI

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node; // Untuk mendapatkan stage dari event
import javafx.event.ActionEvent; // Untuk mendapatkan stage dari event

import java.io.IOException;
import java.net.URL;

public class NavigationUtil {

    private NavigationUtil() {
        // Kelas utilitas tidak seharusnya diinstansiasi
    }

    /**
     * Memuat FXML baru dan mengganti scene pada stage yang diberikan.
     *
     * @param fxmlPath Path ke file FXML (misal, "/org/neracaku/neracaku/fxml/PinView.fxml").
     * @param title Judul untuk stage baru.
     * @param currentStage Stage saat ini yang scenenya akan diganti.
     */
    public static void navigateTo(String fxmlPath, String title, Stage currentStage) {
        try {
            URL fxmlUrl = NavigationUtil.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Tidak dapat menemukan FXML: " + fxmlPath);
                // TODO: Tampilkan dialog error ke pengguna
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);
            // Anda bisa menambahkan stylesheet di sini jika scene baru membutuhkannya dan tidak didefinisikan di FXML
            // scene.getStylesheets().add(NavigationUtil.class.getResource("/org/neracaku/neracaku/css/styles.css").toExternalForm());

            currentStage.setTitle(title);
            currentStage.setScene(scene);
            // Optional: atur ulang ukuran stage jika perlu atau biarkan JavaFX menyesuaikannya
            // currentStage.sizeToScene();
            // currentStage.centerOnScreen(); // Pusatkan stage setelah ukuran berubah
            currentStage.show(); // Pastikan stage tetap terlihat (biasanya tidak perlu jika sudah show sebelumnya)

        } catch (IOException e) {
            System.err.println("Gagal memuat FXML '" + fxmlPath + "': " + e.getMessage());
            e.printStackTrace();
            // TODO: Tampilkan dialog error ke pengguna
        }
    }

    /**
     * Mendapatkan Stage dari sebuah ActionEvent (misalnya dari klik tombol).
     *
     * @param event ActionEvent yang terjadi.
     * @return Stage tempat event terjadi.
     */
    public static Stage getStageFromEvent(ActionEvent event) {
        Node source = (Node) event.getSource();
        return (Stage) source.getScene().getWindow();
    }


    /**
     * Membuka FXML di jendela (Stage) baru.
     *
     * @param fxmlPath Path ke file FXML.
     * @param title Judul untuk stage baru.
     * @param ownerStage Stage pemilik (opsional, jika jendela baru adalah dialog modal).
     *                   Jika null, jendela baru akan menjadi top-level.
     */
    public static void openNewWindow(String fxmlPath, String title, Stage ownerStage) {
        try {
            URL fxmlUrl = NavigationUtil.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Tidak dapat menemukan FXML: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Stage newStage = new Stage();
            newStage.setTitle(title);
            newStage.setScene(new Scene(root));

            if (ownerStage != null) {
                // Jika ini dialog, atur sebagai modal dan pemiliknya
                // newStage.initModality(Modality.WINDOW_MODAL);
                // newStage.initOwner(ownerStage);
            }
            // newStage.setResizable(false); // Opsional
            newStage.show();

        } catch (IOException e) {
            System.err.println("Gagal memuat FXML untuk jendela baru '" + fxmlPath + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Menutup stage yang diberikan.
     * @param stage Stage yang akan ditutup.
     */
    public static void closeStage(Stage stage) {
        if (stage != null) {
            stage.close();
        }
    }
}