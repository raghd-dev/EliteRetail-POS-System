# EliteRetail POS — UI / Brand Redesign Notes

## Design intent

The redesign moves the product away from default JavaFX/student-project styling and toward a commercial desktop SaaS/ERP visual language. The interface prioritizes predictable hierarchy, strong information density, accessible state colors, restrained depth, and consistent component behavior.

## Brand identity

The new mark combines three ideas:

1. **Retail/store operations** — a roof/storefront silhouette.
2. **Sales analytics and growth** — ascending data bars.
3. **Technology and trust** — structured navy geometry with modern blue.

Gold is deliberately limited to the roofline and accent details so the brand feels premium without becoming decorative.

### Deliverable assets

- `brand/elite-retail-logo-full.png` — full logo with `EliteRetail POS`
- `brand/elite-retail-logo-icon.png` — icon-only mark
- `brand/elite-retail-app-icon-64.png` — small application icon
- `brand/elite-retail-app-icon-256.png` — large desktop icon
- `brand/elite-retail-wordmark-dark.png` — wordmark for dark/navy surfaces
- SVG source files for scalable reuse
- Runtime PNG copies in `src/main/resources/images/`

## Color system

- Primary: `#0B1F3A`
- Secondary: `#1D6FE8`
- Accent: `#D9A441`
- Background: `#F4F7FB`
- Surface: `#FFFFFF`
- Border: `#D9E2EE`
- Text: `#132238`
- Muted text: `#617087`
- Success: `#2F855A`
- Warning: `#B7791F`
- Error: `#B4232D`

## Screen redesigns

### Login

- Commercial two-panel sign-in layout
- Branded product story on a deep-navy panel
- Full EliteRetail logo lockup
- Clear form hierarchy and focus states
- Remember-me control added as a non-breaking UI option
- Enter-key login from the password field

### Dashboard

- Replaced the basic button grid with an operations dashboard
- Added revenue, transactions, product-count, and low-stock cards
- Added hourly sales analytics chart
- Added recent-transactions list
- Preserved existing role-based action permissions
- Manager-only sales analytics remain restricted for cashier accounts

### Product Management

- Added consistent app header and page hierarchy
- Consolidated search controls into a toolbar card
- Rebuilt the product table presentation
- Restyled dynamic row action buttons
- Removed the visual duplicate placeholder Actions column; the controller-owned functional Actions column remains
- Restyled product/category dialogs without changing CRUD logic

### Discount Management

- Unified promotions workspace with branded header and toolbar
- Improved discount list readability and state colors
- Restyled add/edit/delete dialog experience
- Preserved all existing DiscountService behavior

### Sales Register

- Rebalanced the layout around product entry and a larger cart table
- Added session/status strip
- Improved cashier identity and bill metadata hierarchy
- Elevated amount-due/payment action as the checkout focal point
- Payment and receipt windows now receive the application brand icon

### Statistics

- Added KPI card layout
- Enlarged analytics chart and top-product table
- Unified refresh/export actions with the enterprise component system

### Receipt

- Added brand mark and professional receipt hierarchy
- Improved transaction metadata, product table, payment summary, discount and total presentation
- Increased receipt preview size for readability
- Updated PDF output with a vector EliteRetail brand mark and branded table header

## CSS system

`src/main/resources/css/styles.css` is now a reusable design system rather than a screen-specific color sheet. It contains component rules for:

- app shells and headers
- typography hierarchy
- cards / surfaces / metric cards
- inputs / combo boxes / date pickers / checkboxes
- primary / secondary / success / danger / navigation buttons
- tables / lists
- charts
- login-specific layout
- sales and receipt patterns
- dialogs and scroll bars

## Micro-interactions

`src/main/java/com/eliteretail/pos/utils/UiEffects.java` adds fast presentation-only transitions:

- short scene fade + vertical entrance
- subtle button hover/press scaling
- optional card lift behavior
- consistent application icon decoration for stages
- consistent CSS application to JavaFX dialogs/alerts

No business logic, persistence rule, or controller action contract is changed by these animations.

## Changed / added files for this redesign

### Branding and documentation

- `brand/README.md`
- `brand/elite-retail-logo-full.png`
- `brand/elite-retail-logo-icon.png`
- `brand/elite-retail-app-icon-64.png`
- `brand/elite-retail-app-icon-256.png`
- `brand/elite-retail-wordmark-dark.png`
- `brand/*.svg`
- `README.md`
- `UI_REDESIGN.md`
- `VALIDATION_REPORT.md`
- `screenshots/login.png`
- `screenshots/dashboard.png`
- `screenshots/products.png`
- `screenshots/sales.png`
- `screenshots/receipt.png`
- `screenshots/statistics.png`

### JavaFX resources

- `src/main/resources/css/styles.css`
- `src/main/resources/fxml/login.fxml`
- `src/main/resources/fxml/dashboard.fxml`
- `src/main/resources/fxml/product-management.fxml`
- `src/main/resources/fxml/discount-management.fxml`
- `src/main/resources/fxml/sales.fxml`
- `src/main/resources/fxml/statistics.fxml`
- `src/main/resources/fxml/payment-dialog.fxml`
- `src/main/resources/fxml/receipt.fxml`
- `src/main/resources/images/pos-logo.png`
- `src/main/resources/images/elite-retail-logo-full.png`
- `src/main/resources/images/elite-retail-wordmark-dark.png`
- `src/main/resources/images/app-icon-16.png`
- `src/main/resources/images/app-icon-32.png`
- `src/main/resources/images/app-icon-64.png`
- `src/main/resources/images/app-icon-128.png`
- `src/main/resources/images/app-icon-256.png`
- `src/main/resources/images/app-icon-512.png`

### UI/controller support

- `src/main/java/com/eliteretail/pos/app/EliteRetailApplication.java`
- `src/main/java/com/eliteretail/pos/controller/LoginController.java`
- `src/main/java/com/eliteretail/pos/controller/DashboardController.java`
- `src/main/java/com/eliteretail/pos/controller/ProductManagementController.java`
- `src/main/java/com/eliteretail/pos/controller/DiscountManagementController.java`
- `src/main/java/com/eliteretail/pos/controller/SalesController.java`
- `src/main/java/com/eliteretail/pos/controller/StatisticsController.java`
- `src/main/java/com/eliteretail/pos/controller/PaymentDialogController.java`
- `src/main/java/com/eliteretail/pos/controller/ReceiptController.java`
- `src/main/java/com/eliteretail/pos/utils/UiEffects.java`
- `src/main/java/com/eliteretail/pos/utils/ReceiptExporter.java`

### Dashboard read model

- `src/main/java/com/eliteretail/pos/model/RecentTransaction.java`
- `src/main/java/com/eliteretail/pos/repository/StatisticsRepository.java`
- `src/main/java/com/eliteretail/pos/service/StatisticsService.java`

The dashboard read-model addition follows the existing repository/service architecture and does not change checkout or database-write behavior.
