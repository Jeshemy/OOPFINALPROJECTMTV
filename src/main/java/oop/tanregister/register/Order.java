package oop.tanregister.register;

import javafx.beans.property.*;

public class Order {
    private StringProperty customerName = new SimpleStringProperty();
    private StringProperty item = new SimpleStringProperty();
    private IntegerProperty quantity = new SimpleIntegerProperty();
    private DoubleProperty price = new SimpleDoubleProperty();
    private StringProperty date = new SimpleStringProperty();
    private StringProperty status = new SimpleStringProperty();

    public Order(String customerName, String item, int quantity, double price, String date, String status) {
        this.customerName.set(customerName);
        this.item.set(item);
        this.quantity.set(quantity);
        this.price.set(price);
        this.date.set(date);
        this.status.set(status);
    }

    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String name) { this.customerName.set(name); }
    public StringProperty customerNameProperty() { return customerName; }

    public String getItem() { return item.get(); }
    public void setItem(String item) { this.item.set(item); }
    public StringProperty itemProperty() { return item; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int qty) { this.quantity.set(qty); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getPrice() { return price.get(); }
    public void setPrice(double price) { this.price.set(price); }
    public DoubleProperty priceProperty() { return price; }

    public String getDate() { return date.get(); }
    public void setDate(String date) { this.date.set(date); }
    public StringProperty dateProperty() { return date; }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }
}
