package ca.ucalgary.chinookair.presentation;

import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** A text field with a magnifier and a clear button that tells its listener about every change. */
class SearchField extends JTextField {

    private static final long serialVersionUID = 1L;

    SearchField(String placeholder, int columns) {
        super(columns);
        putClientProperty("JTextField.placeholderText", placeholder);
        putClientProperty("JTextField.leadingIcon", Icons.muted("search", 16));
        putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        getAccessibleContext().setAccessibleName(placeholder);
    }

    /** Calls the listener with the new text after every typed letter, paste or clear. */
    void onChange(Consumer<String> listener) {
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.accept(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.accept(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.accept(getText());
            }
        });
    }
}
