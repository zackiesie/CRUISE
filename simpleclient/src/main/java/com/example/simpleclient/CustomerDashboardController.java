package com.example.simpleclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

public class CustomerDashboardController {
    @FXML private Label greetingLabel, subtitleLabel, activeCountLabel, totalTripsLabel, spentLabel;
    @FXML private Label heroKickerLabel, heroVisualNumber, heroVisualTitle, heroVisualCaption;
    @FXML private Button slideDot1, slideDot2, slideDot3, pauseButton, featuredButton;
    @FXML private ScrollPane dashboardScroll;
    @FXML private Text heroBodyText;
    @FXML private StackPane heroVisual;
    @FXML private Label bookingTitleLabel, bookingDatesLabel, bookingStatusLabel, bookingAmountLabel;
    @FXML private StackPane bookingActionArea;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private Booking featured;
    private int heroSlide = 0;
    private Timeline heroTimeline;
    private Animation slideTransition;
    private boolean slideshowPaused;
    private final String[][] slides = {
        {"THE CRUISE EXPERIENCE", "City streets or a weekend escape. Find a drive that feels like you.", "01 / THE EVERYDAY ESCAPE", "A little further. A little freer.", "CHOOSE • RESERVE • DRIVE"},
        {"MADE FOR YOUR PLANS", "Choose your dates, protection and extras. Keep every detail in one place.", "02 / YOUR OWN PACE", "Your plans. Your kind of drive.", "FLEXIBLE OPTIONS • CLEAR DAILY RATES"},
        {"A CLEAR ROAD AHEAD", "From payment confirmation to the return, follow every stage of your rental.", "03 / THE NEXT CHAPTER", "Less planning. More possibilities.", "YOUR JOURNEY • ALL IN ONE PLACE"}
    };

    @FXML private void initialize() {
        startHeroSlideshow();
        heroBodyText.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(280, ((javafx.scene.layout.Region) greetingLabel.getParent()).getWidth()),
            ((javafx.scene.layout.Region) greetingLabel.getParent()).widthProperty()));
        dashboardScroll.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == null) {
                heroTimeline.stop();
                if (slideTransition != null) slideTransition.stop();
            } else if (!slideshowPaused) heroTimeline.play();
        });
        featuredButton.setText("Explore the fleet  →");
        if (!AppSession.hasToken()) {
            greetingLabel.setText("Welcome to CRUISE");
            subtitleLabel.setText("Sign in to see your trips and personalised rental overview.");
            bookingTitleLabel.setText("Your next story starts here.");
            bookingDatesLabel.setText("Browse the fleet and sign in when you're ready to book.");
            bookingStatusLabel.setText("GUEST");
            return;
        }
        new Thread(this::load).start();
    }


    private void startHeroSlideshow() {
        heroTimeline = new Timeline(new KeyFrame(Duration.seconds(7), e -> changeSlide((heroSlide + 1) % slides.length)));
        heroTimeline.setCycleCount(Animation.INDEFINITE); heroTimeline.play();
    }
    private void changeSlide(int target) {
        if (slideTransition != null) slideTransition.stop();
        heroSlide = Math.floorMod(target, slides.length);
        String[] slide = slides[heroSlide];
        heroKickerLabel.setText(slide[0]); heroBodyText.setText(slide[1]);
        heroVisualNumber.setText(slide[2]); heroVisualTitle.setText(slide[3]); heroVisualCaption.setText(slide[4]);
        Button[] dots = {slideDot1, slideDot2, slideDot3};
        for (int i = 0; i < dots.length; i++) {
            dots[i].getStyleClass().removeAll("v5-slide-dot", "v5-slide-dot-active");
            dots[i].getStyleClass().add(i == heroSlide ? "v5-slide-dot-active" : "v5-slide-dot");
        }
        FadeTransition fade = new FadeTransition(Duration.millis(420), heroVisual);
        fade.setFromValue(0.45); fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(Duration.millis(420), heroVisual);
        move.setFromX(10); move.setToX(0); move.setInterpolator(Interpolator.EASE_OUT);
        slideTransition = new ParallelTransition(fade, move); slideTransition.play();
    }
    private void manualSlide(int target) { changeSlide(target); if (!slideshowPaused) heroTimeline.playFromStart(); }
    @FXML private void previousSlide() { manualSlide(heroSlide - 1); }
    @FXML private void nextSlide() { manualSlide(heroSlide + 1); }
    @FXML private void firstSlide() { manualSlide(0); }
    @FXML private void secondSlide() { manualSlide(1); }
    @FXML private void thirdSlide() { manualSlide(2); }
    @FXML private void toggleSlideshow() {
        slideshowPaused = !slideshowPaused;
        if (slideshowPaused) heroTimeline.pause(); else heroTimeline.play();
        pauseButton.setText(slideshowPaused ? "Play" : "Pause");
        pauseButton.setAccessibleText(slideshowPaused ? "Play slideshow" : "Pause slideshow");
    }

    private void load() {
        try {
            String token = AppSession.getToken();
            HttpRequest meReq = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/customer/me"))
                    .header("Authorization", "Bearer " + token).GET().build();
            HttpRequest bReq = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/customer/bookings"))
                    .header("Authorization", "Bearer " + token).GET().build();
            HttpResponse<String> me = http.send(meReq, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> br = http.send(bReq, HttpResponse.BodyHandlers.ofString());
            if (me.statusCode() == 401 || br.statusCode() == 401) { AppSession.clearToken(); return; }
            JsonNode user = mapper.readTree(me.body());
            Booking[] bookings = mapper.readValue(br.body(), Booking[].class);
            String name = user.path("name").asText("Driver");
            long active = Arrays.stream(bookings).filter(this::isCurrent).count();
            double spent = Arrays.stream(bookings).filter(b -> "PAID".equalsIgnoreCase(b.getPaymentStatus()) || "REFUNDED".equalsIgnoreCase(b.getPaymentStatus())).mapToDouble(Booking::getTotalAmount).sum();
            featured = Arrays.stream(bookings).filter(this::isCurrent)
                    .min(Comparator.comparing(b -> b.getStartDate() == null ? LocalDate.MAX : b.getStartDate())).orElse(null);
            Platform.runLater(() -> render(name, bookings.length, active, spent));
        } catch (Exception e) {
            Platform.runLater(() -> {
                subtitleLabel.setText("Your overview will return when the connection is restored.");
                bookingTitleLabel.setText("A new journey is waiting.");
                bookingDatesLabel.setText("We couldn't load your upcoming rental. Try My Bookings again shortly.");
                bookingStatusLabel.setText("OFFLINE");
            });
        }
    }

    private boolean isCurrent(Booking b) {
        String s = b.getBookingStatus();
        return s != null && (s.equalsIgnoreCase("ACTIVE") || s.equalsIgnoreCase("RESERVED"));
    }

    private void render(String name, int total, long active, double spent) {
        greetingLabel.setText("Good to see you, " + firstName(name));
        subtitleLabel.setText("Everything about your next drive, in one place.");
        activeCountLabel.setText(String.valueOf(active));
        totalTripsLabel.setText(String.valueOf(total));
        spentLabel.setText(String.format("RM %.0f", spent));
        if (featured == null) {
            bookingTitleLabel.setText("Ready for your next drive?");
            bookingDatesLabel.setText("You have no upcoming rentals right now.");
            bookingStatusLabel.setText("NO UPCOMING BOOKING");
            bookingAmountLabel.setText("Browse cars to get started");
        } else {
            featuredButton.setText("View booking  →");
            bookingTitleLabel.setText("Booking #" + featured.getId());
            bookingDatesLabel.setText(format(featured.getStartDate()) + "  →  " + format(featured.getEndDate()));
            bookingStatusLabel.setText((featured.getBookingStatus() + " • " + featured.getPaymentStatus()).toUpperCase());
            bookingAmountLabel.setText(String.format("RM %.2f • %d day%s", featured.getTotalAmount(), featured.getDays() == null ? 0 : featured.getDays(), featured.getDays() != null && featured.getDays() == 1 ? "" : "s"));
        }
    }

    @FXML private void viewFeaturedBooking() {
        if (featured == null) { browseCars(); return; }
        SelectedBookingSession.setSelectedBooking(featured);
        navigate("booking-details-view.fxml");
    }
    @FXML private void browseCars() { navigate("browse-cars-view.fxml"); }
    @FXML private void viewAllBookings() { navigate("customer-bookings-view.fxml"); }
    private void navigate(String fxml) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxml));
            StackPane host = (StackPane) greetingLabel.getScene().lookup("#contentArea");
            if (host != null) PageMotion.show(host, page);
        } catch (Exception e) { e.printStackTrace(); }
    }
    private String firstName(String name) { return name == null || name.isBlank() ? "Driver" : name.trim().split("\\s+")[0]; }
    private String format(LocalDate d) { return d == null ? "—" : d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")); }
}
