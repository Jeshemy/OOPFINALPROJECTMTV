package oop.tanregister.register;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.bson.Document;
import org.bson.types.Binary;
import javafx.scene.Node;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ResourceBundle;

import static com.mongodb.client.model.Filters.eq;

public class CustomerController implements Initializable {

    @FXML private Button AboutUs;
    @FXML private Button CheckOut;
    @FXML private Button Clear;
    @FXML private Button Contact;
    @FXML private Button Logout;
    @FXML private Button Products;
    @FXML private Button Receipt;
    @FXML private Button Remove;

    @FXML private RadioButton COD;
    @FXML private RadioButton OnlPay;

    @FXML private AnchorPane Checkout;
    @FXML private AnchorPane ProductMenu;
    @FXML private GridPane ProductPane;

    @FXML private TableView<ProductTableData> ProductTable;
    @FXML private TableColumn<ProductTableData, String> colName;
    @FXML private TableColumn<ProductTableData, Double> colPrice;
    @FXML private TableColumn<ProductTableData, Integer> colStock;

    @FXML private TextField Total;
    @FXML private TextField customerNameField;

    private Stage stage;
    private Scene scene;
    private Parent root;

    private ObservableList<ProductTableData> cartList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colPrice != null) colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        if (colStock != null) colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        if (ProductTable != null) ProductTable.setItems(cartList);

        if (COD != null && OnlPay != null) {
            ToggleGroup paymentGroup = new ToggleGroup();
            COD.setToggleGroup(paymentGroup);
            OnlPay.setToggleGroup(paymentGroup);
            COD.setSelected(true);
        }

        if (ProductPane != null) loadProductShowcase();

        if (Clear != null) {
            Clear.setOnAction(e -> {
                cartList.clear();
                updateTotal();
            });
        }
        if (Remove != null && ProductTable != null) {
            Remove.setOnAction(e -> {
                ProductTableData selected = ProductTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    cartList.remove(selected);
                    updateTotal();
                }
            });
        }
    }

    @FXML
    private void handleProducts(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("Customer.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleContactUs(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("ContactUs.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleAboutUs(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("AboutUs.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void loadProductShowcase() {
        if (ProductPane == null) return;

        MongoDatabase db = MongoConnection.getDatabase();
        MongoCollection<Document> collection = db.getCollection("products");

        ProductPane.getChildren().clear();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            int column = 0;
            int row = 0;

            while (cursor.hasNext()) {
                Document doc = cursor.next();

                String name = doc.getString("name");
                Double price = doc.getDouble("price");
                if (price == null) price = 0.0;
                int stock = doc.getInteger("stock", 0);
                Binary imgBinary = doc.get("image", Binary.class);
                byte[] imageBytes = (imgBinary != null) ? imgBinary.getData() : null;

                if (stock <= 0) continue;

                AnchorPane card = createProductCard(name, price, imageBytes);
                ProductPane.add(card, column++, row);

                if (column == 2) {
                    column = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AnchorPane createProductCard(String name, Double price, byte[] imageBytes) {
        MongoDatabase db = MongoConnection.getDatabase();
        MongoCollection<Document> collection = db.getCollection("products");
        Document productDoc = collection.find(eq("name", name)).first();
        int availableStock = (productDoc != null) ? productDoc.getInteger("stock", 0) : 0;

        AnchorPane card = new AnchorPane();
        card.setPrefSize(200, 200);
        card.setStyle("-fx-background-color: #2f6690; -fx-background-radius: 10;");

        Label nameLabel = new Label(name);
        nameLabel.setLayoutX(15);
        nameLabel.setLayoutY(15);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'Montserrat Black'; -fx-font-size: 12;");

        Label priceLabel = new Label("PHP " + String.format("%.2f", price));
        priceLabel.setLayoutX(120);
        priceLabel.setLayoutY(15);
        priceLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'Montserrat Black'; -fx-font-size: 12;");

        ImageView imgView = new ImageView();
        imgView.setFitWidth(170);
        imgView.setFitHeight(100);
        imgView.setLayoutX(15);
        imgView.setLayoutY(35);
        imgView.setPreserveRatio(true);
        if (imageBytes != null) {
            imgView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        }

        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, availableStock > 0 ? availableStock : 1, 1));
        spinner.setLayoutX(20);
        spinner.setLayoutY(140);
        spinner.setPrefWidth(90);

        Button addButton = new Button("ADD");
        addButton.setLayoutX(120);
        addButton.setLayoutY(140);
        addButton.setPrefSize(60, 25);
        addButton.setStyle("-fx-background-color: #fefcfb;");
        addButton.setOnAction(e -> addToCart(name, price, spinner.getValue(), imageBytes));

        card.getChildren().addAll(nameLabel, priceLabel, imgView, spinner, addButton);
        return card;
    }

    private void addToCart(String name, double price, int quantity, byte[] imageBytes) {
        MongoDatabase db = MongoConnection.getDatabase();
        MongoCollection<Document> collection = db.getCollection("products");
        Document productDoc = collection.find(eq("name", name)).first();

        int availableStock = (productDoc != null) ? productDoc.getInteger("stock", 0) : 0;

        ProductTableData existing = null;
        for (ProductTableData item : cartList) {
            if (item.getName().equals(name)) {
                existing = item;
                break;
            }
        }

        int totalRequested = quantity + (existing != null ? existing.getStock() : 0);
        if (totalRequested > availableStock) {
            showAlert(Alert.AlertType.WARNING, "Insufficient Stock",
                    "Only " + availableStock + " units of " + name + " available.");
            return;
        }

        if (existing != null) {
            int newStock = existing.getStock() + quantity;
            cartList.remove(existing);
            cartList.add(new ProductTableData(name, price, newStock, imageBytes));
        } else {
            cartList.add(new ProductTableData(name, price, quantity, imageBytes));
        }

        if (ProductTable != null) ProductTable.refresh();
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (ProductTableData item : cartList) {
            total += item.getPrice() * item.getStock();
        }
        if (Total != null) {
            Total.setText(String.format("PHP %.2f", total));
        }
    }

    @FXML
    void handleCheckout(ActionEvent event) {
        try {
            String name = (oop.tanregister.db.UserData.getCurrentUsername() != null)
                    ? oop.tanregister.db.UserData.getCurrentUsername()
                    : "Guest";


            if (cartList.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Your cart is empty!");
                return;
            }

            MongoDatabase database = MongoConnection.getDatabase();
            MongoCollection<Document> orders = database.getCollection("orders");

            double totalPrice = 0;

            for (ProductTableData item : cartList) {
                Document order = new Document("customerName", name)
                        .append("item", item.getName())
                        .append("quantity", item.getStock())
                        .append("price", item.getPrice())
                        .append("date", LocalDateTime.now().toString());

                orders.insertOne(order);
                totalPrice += item.getPrice() * item.getStock();
            }

            showAlert(Alert.AlertType.INFORMATION,
                    "Checked Out!",
                    "Thank you for your purchase, " + name + "!\nTotal: ₱" + String.format("%.2f", totalPrice));

            cartList.clear();
            updateTotal();
            if (ProductPane != null) loadProductShowcase();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error placing order: " + e.getMessage());
        }
    }

    public void handleReceipt(ActionEvent event) {
        if (cartList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Items", "No items in cart to print.");
            return;
        }

        try {
            File file = new File("receipt_" + System.currentTimeMillis() + ".txt");
            FileWriter writer = new FileWriter(file);

            writer.write("======== PRINTSONALIZED RECEIPT ========\n");
            writer.write("Date: " + new Date() + "\n");
            writer.write("Payment Method: " + (COD != null && COD.isSelected() ? "Cash on Delivery" : "Online Payment") + "\n");
            writer.write("----------------------------------------\n");
            writer.write(String.format("%-20s %-10s %-10s %-10s\n", "Product", "Price", "Qty", "Subtotal"));

            double total = 0;
            for (ProductTableData item : cartList) {
                double subtotal = item.getPrice() * item.getStock();
                total += subtotal;
                writer.write(String.format("%-20s %-10.2f %-10d %-10.2f\n",
                        item.getName(), item.getPrice(), item.getStock(), subtotal));
            }

            writer.write("----------------------------------------\n");
            writer.write(String.format("Total: PHP %.2f\n", total));
            writer.write("========================================\n");
            writer.write("Thank you for shopping with us!\n");

            writer.close();

            showAlert(Alert.AlertType.INFORMATION, "Receipt Generated",
                    "Receipt saved as:\n" + file.getAbsolutePath());

            java.awt.Desktop.getDesktop().open(file);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate receipt.");
        }
    }

    public void handleLogout(ActionEvent event) {
        try {
            if (Logout != null) {
                Stage currentStage = (Stage) Logout.getScene().getWindow();
                currentStage.close();

                Login loginApp = new Login();
                Stage stage = new Stage();
                loginApp.start(stage);
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open login page:\n" + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String message) {
        showAlert(type, "Notification", message);
    }
}
