package ca.ucalgary.chinookair.presentation;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.util.List;

/**
 * The small line icons of the program. They are SVG files in the icons folder, drawn in black; this
 * class gives them a size and the colour the screen wants, so they stay sharp on every display.
 */
final class Icons {

    /** The file names, without ".svg". */
    static final List<String> NAMES = List.of("search", "calendar", "swap", "check", "alert", "close", "plane",
            "ticket", "user", "copy", "folder", "chevron-left", "chevron-right", "card", "grid");

    private Icons() {
    }

    static FlatSVGIcon of(String name, int size, Color color) {
        if (!NAMES.contains(name)) {
            throw new IllegalArgumentException("Unknown icon " + name);
        }
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(any -> color));
        return icon;
    }

    /** An icon in the muted text colour, for fields and quiet buttons. */
    static FlatSVGIcon muted(String name, int size) {
        return of(name, size, Theme.MUTED);
    }
}
