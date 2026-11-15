package ca.ucalgary.chinookair.presentation;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Keeps characters out of a box that could never be part of what it asks for, like a letter in a
 * number. A box only drops what cannot be valid; whether what is left is right is still decided
 * when the form is sent.
 */
final class InputFilters {

    private InputFilters() {
    }

    /** Only digits, at most {@code maxLength} of them. */
    static void digits(JTextComponent field, int maxLength) {
        only(field, "0-9", maxLength);
    }

    /** Only the characters in the class (like {@code "0-9/"}), at most {@code maxLength}; others are dropped. */
    static void only(JTextComponent field, String characterClass, int maxLength) {
        String keep = "[^" + characterClass + "]";
        install(field, new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
                    throws BadLocationException {
                replace(bypass, offset, 0, text, attributes);
            }

            @Override
            public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                    throws BadLocationException {
                String clean = text == null ? "" : text.replaceAll(keep, "");
                int room = maxLength - (bypass.getDocument().getLength() - length);
                if (clean.length() > room) {
                    clean = clean.substring(0, Math.max(room, 0));
                }
                if (!clean.isEmpty() || length > 0) {
                    super.replace(bypass, offset, length, clean, attributes);
                }
            }
        });
    }

    /** A number with at most one decimal point and no sign, at most {@code maxLength} characters. */
    static void decimal(JTextComponent field, int maxLength) {
        install(field, new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
                    throws BadLocationException {
                replace(bypass, offset, 0, text, attributes);
            }

            @Override
            public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                    throws BadLocationException {
                String current = bypass.getDocument().getText(0, bypass.getDocument().getLength());
                String result = current.substring(0, offset) + (text == null ? "" : text)
                        + current.substring(offset + length);
                if (result.matches("\\d*\\.?\\d*") && result.length() <= maxLength) {
                    super.replace(bypass, offset, length, text, attributes);
                }
            }
        });
    }

    private static void install(JTextComponent field, DocumentFilter filter) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);
    }
}
