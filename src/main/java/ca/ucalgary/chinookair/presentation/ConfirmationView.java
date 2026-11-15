package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The last step of booking: a clear "it worked", the confirmation number to copy, the trip, and where
 * the copy of the confirmation was saved.
 */
class ConfirmationView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int WIDTH = 640;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JButton dashboardButton = Theme.primary(new JButton("Done"));
    final JButton anotherButton = Theme.secondary(new JButton("Book another flight"));
    final JButton copyButton = Theme.secondary(new JButton("Copy"));
    final JButton folderButton = Theme.link(new JButton("Open folder"));
    private final JLabel title = new JLabel("Booking confirmed");
    private final JLabel subtitle = Theme.hint(" ");
    private final JLabel number = new JLabel(" ");
    private final JLabel saved = Theme.hint(" ");
    private final JPanel tripHolder = new JPanel(new BorderLayout());
    private SummaryCard trip = new SummaryCard("Your trip");
    private transient Consumer<String> clipboard = ConfirmationView::copyToSystemClipboard;
    private transient Consumer<Path> folderOpener = ConfirmationView::openInFileManager;
    private Path file;

    ConfirmationView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout());
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE, Theme.PAGE, Theme.PAGE, Theme.PAGE));
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        // the good news first
        JLabel check = new JLabel(Icons.of("check", 22, Color.WHITE));
        check.setOpaque(true);
        check.setBackground(Theme.SUCCESS);
        check.setHorizontalAlignment(JLabel.CENTER);
        check.setPreferredSize(new java.awt.Dimension(40, 40));
        title.setFont(title.getFont().deriveFont(Font.BOLD, Type.PAGE_HEADING));
        title.setForeground(Theme.TEXT);
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(title);
        words.add(subtitle);
        JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        banner.setOpaque(false);
        banner.add(check);
        banner.add(words);

        // the number to keep, on a card with one gold edge
        JLabel caption = Theme.hint("Confirmation number");
        number.setFont(number.getFont().deriveFont(Font.BOLD, Type.DISPLAY));
        number.setForeground(Palette.NAVY_900);
        number.getAccessibleContext().setAccessibleName("Confirmation number");
        JPanel numberWords = new JPanel();
        numberWords.setOpaque(false);
        numberWords.setLayout(new BoxLayout(numberWords, BoxLayout.Y_AXIS));
        numberWords.add(caption);
        numberWords.add(number);
        JPanel numberCard = new JPanel(new BorderLayout(16, 0));
        numberCard.setBackground(Theme.SURFACE);
        numberCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, Palette.GOLD_500),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        numberCard.add(numberWords, BorderLayout.CENTER);
        numberCard.add(copyButton, BorderLayout.EAST);
        copyButton.setIcon(Icons.muted("copy", 16));
        copyButton.setMnemonic('C');

        tripHolder.setOpaque(false);
        tripHolder.add(trip, BorderLayout.CENTER);

        JPanel savedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        savedRow.setOpaque(false);
        savedRow.add(saved);
        savedRow.add(folderButton);

        anotherButton.setMnemonic('B');
        dashboardButton.setMnemonic('D');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(dashboardButton);
        buttons.add(anotherButton);

        for (JPanel part : List.of(banner, numberCard, tripHolder, savedRow, buttons)) {
            part.setAlignmentX(LEFT_ALIGNMENT);
            part.setMaximumSize(new java.awt.Dimension(WIDTH, Integer.MAX_VALUE));
        }
        column.add(banner);
        column.add(Box.createVerticalStrut(20));
        column.add(numberCard);
        column.add(Box.createVerticalStrut(16));
        column.add(tripHolder);
        column.add(Box.createVerticalStrut(8));
        column.add(savedRow);
        column.add(Box.createVerticalStrut(16));
        column.add(buttons);
        add(column, BorderLayout.NORTH);

        dashboardButton.addActionListener(e -> backToDashboard());
        anotherButton.addActionListener(e -> bookAnother());
        copyButton.addActionListener(e -> copyNumber());
        folderButton.addActionListener(e -> {
            if (file != null) {
                folderOpener.accept(file.getParent());
            }
        });
    }

    private void backToDashboard() {
        booking.clear();
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
    }

    private void bookAnother() {
        booking.clear();
        navigator.show(ViewId.FLIGHT_SEARCH);
    }

    private void copyNumber() {
        clipboard.accept(number.getText());
        copyButton.setText("Copied");
        Timer timer = new Timer(2000, e -> copyButton.setText("Copy"));
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        Optional<PaymentResult> result = booking.payment();
        if (result.isEmpty()) {
            navigator.show(ViewId.FLIGHT_SEARCH);
            return;
        }
        show(result.get());
    }

    private void show(PaymentResult result) {
        Reservation reservation = result.reservation();
        Flight flight = reservation.flight();
        number.setText(reservation.confirmationNumber());
        copyButton.setText("Copy");
        file = result.confirmationFile();
        subtitle.setText("Thank you, " + reservation.customer().name() + ". The amount has been paid.");
        saved.setText(file != null ? "A copy of this confirmation was saved: " + file.getFileName()
                : result.message());
        folderButton.setVisible(file != null && file.getParent() != null);

        tripHolder.removeAll();
        trip = new SummaryCard("Your trip")
                .addRow("Passenger", reservation.customer().name())
                .addRow("Flight", flight.flightNumber() + " (" + flight.airline().name() + ")")
                .addRow("Route", routeOf(flight))
                .addRow("Departs", flight.departure().format(WHEN))
                .addRow("Seat", reservation.seatNo())
                .addTotal("Amount paid", "$" + result.payment().amount().toPlainString());
        tripHolder.add(trip, BorderLayout.CENTER);
        tripHolder.revalidate();
        tripHolder.repaint();
    }

    private static String routeOf(Flight flight) {
        return flight.route().origin().code() + " → " + flight.route().destination().code();
    }

    private static void copyToSystemClipboard(String text) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (IllegalStateException e) {
            // another program holds the clipboard for a moment; the number is still on the screen
        }
    }

    private static void openInFileManager(Path folder) {
        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return;
        }
        try {
            Desktop.getDesktop().open(folder.toFile());
        } catch (IOException | IllegalArgumentException e) {
            // nothing to do: the file name is shown on the screen anyway
        }
    }

    // ---- for the tests

    String numberText() {
        return number.getText();
    }

    String subtitleText() {
        return subtitle.getText();
    }

    String savedText() {
        return saved.getText();
    }

    List<SummaryCard.Row> tripRows() {
        return trip.rows();
    }

    void setClipboard(Consumer<String> clipboard) {
        this.clipboard = clipboard;
    }

    void setFolderOpener(Consumer<Path> opener) {
        this.folderOpener = opener;
    }
}
