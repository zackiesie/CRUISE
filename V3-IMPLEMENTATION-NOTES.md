# CRUISE V3 — Desktop Experience & Assignment Compliance

This version preserves the working V2 authentication, Stripe webhook, customer booking and admin security flows while expanding the visible JavaFX experience.

## V3 additions
- Centralised desktop window behaviour (`WindowManager`): resizable main window, consistent minimum size, consistent navigation scene sizing.
- Booking add-ons: child seat and additional driver.
- Protection selection: Standard / Plus / Premium, included in the Stripe checkout total.
- Booking price breakdown persisted with the booking.
- Booking journey/timeline and return record on Booking Details.
- Saveable text payment receipt from Booking Details.
- Customer Activity / Notifications centre.
- Admin vehicle maintenance state, mileage and next-service date.
- Admin dashboard maintenance count.
- Return processing captures mileage, fuel level and notes before marking a paid rental completed.
- Vehicles marked `MAINTENANCE` or `UNAVAILABLE` cannot be checked out.
- Desktop menu and tooltips added for clearer desktop interaction.

## SWC4453 JavaFX control checklist
The project visibly uses the required controls:
- Text
- Label
- Button
- TextField
- PasswordField
- RadioButton
- CheckBox
- Menu
- ToolTip

The application contains substantially more than five connected interfaces.

## Responsive desktop policy
CRUISE is intentionally desktop-first. Main windows are resizable and share one minimum size so layouts do not collapse into a mobile-like presentation. Content-heavy screens use ScrollPane and growable JavaFX layout containers. The same window is reused when navigating between customer, vehicle detail and admin screens.

## Before submission
Run both builds with JDK 21:

```bat
cd simpleapi
mvn clean test
```

```bat
cd ..\simpleclient
mvn clean package
```

Then run the backend, Stripe listener and JavaFX client and test one complete payment + return lifecycle.
