package oop.tanregister.register;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.bson.Document;
import javafx.scene.control.cell.ComboBoxTableCell;
import java.net.URL;
import java.util.ResourceBundle;
import static com.mongodb.client.model.Filters.eq;
public class AdCustomerController implements Initializable {

    @FXML private AnchorPane sidebar;
    @FXML private Button inventoryButton;
    @FXML private Button mainButton;
    @FXML private Button customerButton;
    @FXML private Button signOutButton;

    @FXML private AnchorPane mainPanel;
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> userID;
    @FXML private TableColumn<Order, String> productID;
    @FXML private TableColumn<Order, Double> orderTotal;
    @FXML private TableColumn<Order, String> productDate;
    @FXML private TableColumn<Order, String> productStatus;

    private ObservableList<Order> orderList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userID.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        productID.setCellValueFactory(new PropertyValueFactory<>("item"));
        orderTotal.setCellValueFactory(new PropertyValueFactory<>("price"));
        productDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        productStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        productStatus.setCellFactory(ComboBoxTableCell.forTableColumn("Delivered", "To Pack", "Shipped"));
        productStatus.setOnEditCommit(event -> {
            Order order = event.getRowValue();
            order.setStatus(event.getNewValue());
            updateOrderStatusInDB(order);
        });

        orderTable.setItems(orderList);
        orderTable.setEditable(true);

        loadOrders();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> loadOrders()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateOrderStatusInDB(Order order) {
        try {
            MongoDatabase database = MongoConnection.getDatabase();
            MongoCollection<Document> orders = database.getCollection("orders");
            orders.updateOne(
                    eq("customerName", order.getCustomerName()),
                    new Document("$set", new Document("status", order.getStatus()))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOrders() {
        orderList.clear();
        MongoDatabase database = MongoConnection.getDatabase();
        MongoCollection<Document> orders = database.getCollection("orders");

        try (MongoCursor<Document> cursor = orders.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();

                String customerName = doc.getString("customerName");
                String item = doc.getString("item");
                Integer quantity = doc.getInteger("quantity", 0);
                Double price = doc.getDouble("price");
                if (price == null && doc.get("price") instanceof Number) {
                    price = ((Number) doc.get("price")).doubleValue();
                }
                String date = doc.getString("date");
                String status = doc.getString("status");

                Order order = new Order(customerName, item, quantity, price, date, status);
                orderList.add(order);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        orderTable.setItems(orderList);
    }

    @FXML
    private void switchInventory() {
        System.out.println("Inventory clicked");
    }

    @FXML
    private void switchMenuMenu() {
        System.out.println("Main Menu clicked");
    }

    @FXML
    private void switchCustomer() {
        System.out.println("Customers clicked");
        loadOrders();
    }

    @FXML
    private void handleSignOut() {
        System.out.println("Sign Out clicked");
    }

    public void handleInventory(MouseEvent mouseEvent) {
    }
}
