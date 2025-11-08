package oop.tanregister.register;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import oop.tanregister.model.Product;
import org.bson.Document;
import org.bson.types.Binary;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class AdminMenuController implements Initializable {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productID;
    @FXML private TableColumn<Product, String> productName;
    @FXML private TableColumn<Product, String> productType;
    @FXML private TableColumn<Product, Integer> ProductStock;
    @FXML private TableColumn<Product, Double> ProductPrice;
    @FXML private TableColumn<Product, String> ProductStatus;
    @FXML private TableColumn<Product, Date> ProductDate;

    @FXML private TextField IDField;
    @FXML private TextField ProductNameField;
    @FXML private ComboBox<String> TypeField;
    @FXML private TextField StockField;
    @FXML private TextField Price;
    @FXML private ComboBox<String> StatusField;
    @FXML private Button mainButton;
    @FXML private Button inventoryButton;
    @FXML private Button customerButton;
    @FXML private Button UpdateButton;
    @FXML private Button ClearButton;
    @FXML private Button DeleteButton;
    @FXML private Button importButton;
    @FXML private Button SignOut;
    @FXML private ImageView productImageView;

    private byte[] currentImageBytes;
    private Stage stage;
    private Scene scene;
    private Parent root;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TypeField.getItems().addAll("Paper Prints", "Documents", "Merch Prints", "Stationary", "Souvenirs", "Packaging");
        StatusField.getItems().addAll("Completed", "Delivered", "To Pack");

        productID.setCellValueFactory(new PropertyValueFactory<>("id"));
        productName.setCellValueFactory(new PropertyValueFactory<>("name"));
        productType.setCellValueFactory(new PropertyValueFactory<>("type"));
        ProductStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        ProductPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        ProductDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        ProductStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        ProductStatus.setCellFactory(ComboBoxTableCell.forTableColumn("Delivered", "To Pack", "Shipped"));
        ProductStatus.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setStatus(event.getNewValue());
            updateProductStatusInDB(p);
        });
        productTable.setEditable(true);

        loadProducts();

        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                IDField.setText(newSelection.getId());
                ProductNameField.setText(newSelection.getName());
                TypeField.setValue(newSelection.getType());
                StockField.setText(String.valueOf(newSelection.getStock()));
                Price.setText(String.valueOf(newSelection.getPrice()));
                StatusField.setValue(newSelection.getStatus());
                currentImageBytes = newSelection.getImageBytes();

                if (currentImageBytes != null && currentImageBytes.length > 0) {
                    productImageView.setImage(new Image(new ByteArrayInputStream(currentImageBytes)));
                } else {
                    productImageView.setImage(null);
                }
            }
        });
    }

    private void updateProductStatusInDB(Product p) {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            MongoCollection<Document> collection = db.getCollection("products");
            collection.updateOne(
                    eq("productID", p.getId()),
                    new Document("$set", new Document("status", p.getStatus()))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        MongoDatabase db = MongoConnection.getDatabase();
        MongoCollection<Document> collection = db.getCollection("products");
        ObservableList<Product> productList = FXCollections.observableArrayList();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Product p = new Product();

                p.setId(doc.getString("productID"));
                p.setName(doc.getString("name"));
                p.setType(doc.getString("type"));
                p.setStock(doc.getInteger("stock", 0));
                p.setPrice(doc.getDouble("price"));
                p.setStatus(doc.getString("status"));
                p.setDate(doc.getDate("date"));

                Binary img = doc.get("image", Binary.class);
                if (img != null) p.setImageBytes(img.getData());

                productList.add(p);
            }
        }
        productTable.setItems(productList);
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        String id = IDField.getText().trim();
        String name = ProductNameField.getText().trim();
        String type = (TypeField.getValue() != null) ? TypeField.getValue() : "";
        String stockText = StockField.getText().trim();
        String priceText = Price.getText().trim();
        String status = (StatusField.getValue() != null) ? StatusField.getValue() : "";

        if (id.isEmpty() || name.isEmpty() || type.isEmpty() || stockText.isEmpty() || priceText.isEmpty() || status.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing Information", "Please fill in all fields including Product ID.");
            return;
        }

        try {
            int stock = Integer.parseInt(stockText);
            double price = Double.parseDouble(priceText);

            MongoDatabase db = MongoConnection.getDatabase();
            MongoCollection<Document> collection = db.getCollection("products");

            Document existing = collection.find(eq("productID", id)).first();
            if (existing != null) {
                showAlert(Alert.AlertType.ERROR, "Duplicate ID", "A product with this ID already exists.");
                return;
            }

            Document doc = new Document("productID", id)
                    .append("name", name)
                    .append("type", type)
                    .append("stock", stock)
                    .append("price", price)
                    .append("status", status)
                    .append("date", new Date())
                    .append("image", currentImageBytes != null ? new Binary(currentImageBytes) : null);

            collection.insertOne(doc);

            loadProducts();
            clearForm();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Product added successfully!");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Stock and Price must be numbers.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a product to update.");
            return;
        }

        String newId = IDField.getText().trim();
        if (newId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing ID", "Product ID cannot be empty.");
            return;
        }

        try {
            MongoDatabase db = MongoConnection.getDatabase();
            MongoCollection<Document> collection = db.getCollection("products");

            if (!newId.equals(selected.getId())) {
                Document existing = collection.find(eq("productID", newId)).first();
                if (existing != null) {
                    showAlert(Alert.AlertType.ERROR, "Duplicate ID", "A product with this ID already exists.");
                    return;
                }
            }

            Document update = new Document("productID", newId)
                    .append("name", ProductNameField.getText())
                    .append("type", TypeField.getValue())
                    .append("stock", Integer.parseInt(StockField.getText()))
                    .append("price", Double.parseDouble(Price.getText()))
                    .append("status", StatusField.getValue())
                    .append("date", new Date())
                    .append("image", currentImageBytes != null ? new Binary(currentImageBytes) : null);

            collection.updateOne(eq("productID", selected.getId()), new Document("$set", update));

            loadProducts();
            showAlert(Alert.AlertType.INFORMATION, "Updated", "Product updated successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Update Error", e.getMessage());
        }
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "No Selection", "Please select a product to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete product " + selected.getId() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            MongoDatabase db = MongoConnection.getDatabase();
            MongoCollection<Document> collection = db.getCollection("products");
            collection.deleteOne(eq("productID", selected.getId()));
            loadProducts();
            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Deleted", "Product deleted successfully!");
        }
    }

    @FXML
    public void handleImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] imageBytes = fis.readAllBytes();
                fis.close();

                currentImageBytes = imageBytes;
                productImageView.setImage(new Image(file.toURI().toString()));

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "File Error", "Could not load image.");
            }
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        clearForm();
    }

    private void clearForm() {
        IDField.clear();
        ProductNameField.clear();
        TypeField.getSelectionModel().clearSelection();
        StockField.clear();
        Price.clear();
        StatusField.getSelectionModel().clearSelection();
        productImageView.setImage(null);
        currentImageBytes = null;
    }

    @FXML
    public void handleSignOut(MouseEvent event) throws Exception {
        Stage currentStage = (Stage) SignOut.getScene().getWindow();
        currentStage.close();
        Login loginApp = new Login();
        Stage stage = new Stage();
        loginApp.start(stage);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void switchCustomer(ActionEvent event) throws IOException {
        try {
            URL fxmlUrl = getClass().getResource("/oop/tanregister/register/adcustomer.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "File Error", "Customer page file not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(param -> {
                try {
                    return param == AdminMenuController.class ? this : param.newInstance();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            });

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Customer");
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Cannot load customer page: " + e.getMessage());
        }
    }

    @FXML
    public void switchInventory(ActionEvent event) throws IOException {
        try {
            URL fxmlUrl = getClass().getResource("/oop/tanregister/register/adminmenu.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "File Error", "Inventory file not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(param -> {
                try {
                    return param == AdminMenuController.class ? this : param.newInstance();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            });

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Inventory");
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Cannot load inventory: " + e.getMessage());
        }
    }

    @FXML
    public void switchMenuMenu(ActionEvent event) throws IOException {
        try {
            URL fxmlUrl = getClass().getResource("/oop/tanregister/register/admainview.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "File Error", "Main menu file not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(param -> {
                try {
                    return param == AdminMenuController.class ? this : param.newInstance();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            });

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Main Menu");
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Cannot load main menu: " + e.getMessage());
        }
    }

    @FXML
    public void switchToCustomerView(ActionEvent event) throws IOException {
        try {
            URL fxmlUrl = getClass().getResource("/oop/tanregister/register/Customer.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "File Error", "Customer view file not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(param -> {
                try {
                    return param == AdminMenuController.class ? this : param.newInstance();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            });

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Customer View");
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Cannot load customer view: " + e.getMessage());
        }
    }

    @FXML
    void handleCustomerButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oop/tanregister/register/adcustomer.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Customer Orders");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshProductsFromCustomer() {
        loadProducts();
    }
}
