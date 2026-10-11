# CRUISE — unified desktop design
Customer and operations screens share warm ivory, forest charcoal and champagne, consistent controls, surfaces, typography and restrained motion.
- Reconciled accumulated stylesheet rules and aligned legacy screens.
- Home uses original scalable vehicle artwork and a controllable, pausable carousel.
- Fleet card columns adapt to available desktop width.
- Navigation follows child-page destinations; scene changes preserve window size.
Original edited files are retained in .design-backup.

Validation completed:
- Maven package succeeded under JDK 21.
- 26 JavaFX runtime renders passed across customer widths 1280/1000 and admin workspace widths 1045/765.
- Navigation, child-page active state, fleet filters, manual carousel slides and pause/play passed.
- Native desktop review confirmed the home overview and fleet use live backend data.
- Render fixtures for vehicle/booking detail pages are local preview objects and do not create bookings or payments.

The responsive vehicle editor stacks at narrow workspace widths. Window transitions preserve scene size and maximized state. UI review files are in .design-review and excluded from Git alongside original backups.
