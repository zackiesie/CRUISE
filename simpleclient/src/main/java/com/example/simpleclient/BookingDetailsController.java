package com.example.simpleclient;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.time.format.DateTimeFormatter;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class BookingDetailsController {
    @FXML private Label idLabel, statusLabel, paymentLabel, datesLabel, durationLabel, totalLabel, customerLabel, emailLabel, phoneLabel, carIdLabel;
    @FXML private Label baseAmountLabel, addonsLabel, protectionLabel, addonAmountLabel, returnLabel, timelineRentalLabel, timelineReturnLabel;
    private Booking booking;
    @FXML private void initialize() {
        booking = SelectedBookingSession.getSelectedBooking();
        if (booking == null) { idLabel.setText("Booking unavailable"); return; }
        idLabel.setText("Booking #" + booking.getId());
        statusLabel.setText(value(booking.getBookingStatus()));
        paymentLabel.setText(value(booking.getPaymentStatus()));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy");
        datesLabel.setText((booking.getStartDate()==null?"—":booking.getStartDate().format(f)) + "  →  " + (booking.getEndDate()==null?"—":booking.getEndDate().format(f)));
        durationLabel.setText((booking.getDays()==null?0:booking.getDays()) + " rental day" + (booking.getDays()!=null && booking.getDays()==1?"":"s"));
        totalLabel.setText(String.format("RM %.2f", booking.getTotalAmount()));
        customerLabel.setText(value(booking.getCustomerName())); emailLabel.setText(value(booking.getCustomerEmail())); phoneLabel.setText(value(booking.getCustomerPhone()));
        carIdLabel.setText("Vehicle reference #" + booking.getCarId());
        baseAmountLabel.setText(String.format("RM %.2f", booking.getBaseAmount() > 0 ? booking.getBaseAmount() : booking.getTotalAmount()));
        addonAmountLabel.setText(String.format("RM %.2f", booking.getAddOnAmount()));
        addonsLabel.setText(value(booking.getAddOns()));
        protectionLabel.setText(value(booking.getProtectionPlan()));
        boolean completed = "COMPLETED".equalsIgnoreCase(booking.getBookingStatus());
        timelineRentalLabel.setText(completed ? "✓ Rental" : "Rental active");
        timelineReturnLabel.setText(completed ? "✓ Returned" : "Return");
        if (completed) {
            returnLabel.setText("Returned " + (booking.getCompletedAt()==null?"":booking.getCompletedAt().toLocalDate())
                    + "  •  Mileage: " + (booking.getReturnMileage()==null?"—":booking.getReturnMileage()+" km")
                    + "  •  Fuel: " + value(booking.getReturnFuelLevel())
                    + (booking.getReturnNotes()==null||booking.getReturnNotes().isBlank()?"":"\nNotes: "+booking.getReturnNotes()));
        }
    }
    @FXML private void saveReceipt() {
        if (booking == null) return;
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save CRUISE Receipt");
            chooser.setInitialFileName("CRUISE-Booking-" + booking.getId() + "-Receipt.txt");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text receipt", "*.txt"));
            File file = chooser.showSaveDialog(idLabel.getScene().getWindow());
            if (file == null) return;
            String receipt = "CRUISE CAR RENTAL\nPAYMENT RECEIPT\n\nBooking #" + booking.getId()
                    + "\nCustomer: " + value(booking.getCustomerName())
                    + "\nEmail: " + value(booking.getCustomerEmail())
                    + "\nRental: " + booking.getStartDate() + " to " + booking.getEndDate()
                    + "\nBase rental: RM " + String.format("%.2f", booking.getBaseAmount() > 0 ? booking.getBaseAmount() : booking.getTotalAmount())
                    + "\nAdd-ons: " + value(booking.getAddOns())
                    + "\nProtection: " + value(booking.getProtectionPlan())
                    + "\nExtras: RM " + String.format("%.2f", booking.getAddOnAmount())
                    + "\nTOTAL: RM " + String.format("%.2f", booking.getTotalAmount())
                    + "\nPayment: " + value(booking.getPaymentStatus())
                    + "\nStatus: " + value(booking.getBookingStatus()) + "\n";
            Files.writeString(file.toPath(), receipt, StandardCharsets.UTF_8);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void backToBookings() { navigate("customer-bookings-view.fxml"); }
    @FXML private void backToDashboard() { navigate("customer-dashboard-view.fxml"); }
    private void navigate(String fxml) { try { Node p=FXMLLoader.load(getClass().getResource(fxml)); StackPane host=(StackPane)idLabel.getScene().lookup("#contentArea"); if(host!=null)host.getChildren().setAll(p); } catch(Exception e){e.printStackTrace();} }
    private String value(String s){return s==null||s.isBlank()?"—":s;}
}
