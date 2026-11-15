package ca.ucalgary.chinookair.presentation;

import java.awt.Color;

/**
 * Every colour of the program, taken from the ChinookAir logo. This is the only place with colour
 * numbers; the theme file and the views use these names (a test checks that). Light colours are never
 * used for text: sky and gold are too pale on white, so they only fill shapes and draw lines.
 */
final class Palette {

    // the mountain, the name and the plane
    static final Color NAVY_900 = new Color(0x093151);
    static final Color NAVY_950 = new Color(0x082848);

    // the "Air" in the name, in three strengths; blue-700 is the one used for text and buttons
    static final Color BLUE_600 = new Color(0x0779C8);
    static final Color BLUE_700 = new Color(0x066AB0);
    static final Color BLUE_800 = new Color(0x055A96);
    static final Color BLUE_900 = new Color(0x044A7C);

    // the sky trail and the snow on the mountain
    static final Color SKY_500 = new Color(0x2DB1E9);
    static final Color SKY_100 = new Color(0xE6F3FC);

    // the sun
    static final Color GOLD_500 = new Color(0xFDB52E);
    static final Color GOLD_100 = new Color(0xFCE7A8);

    // surfaces and lines
    static final Color PAGE = new Color(0xF5F8FB);
    static final Color SURFACE = new Color(0xFFFFFF);
    static final Color BORDER = new Color(0xD9E2EC);
    static final Color BORDER_STRONG = new Color(0xC9D3E0);
    static final Color TABLE_HEADER = new Color(0xEEF3F8);
    static final Color ROW_HOVER = new Color(0xF5F9FD);
    static final Color TABLE_LINE = new Color(0xEEF1F4);
    static final Color TAKEN = new Color(0xE4E9EF);

    // text
    static final Color TEXT = new Color(0x102A43);
    static final Color TEXT_MUTED = new Color(0x52606D);
    static final Color DISABLED_TEXT = new Color(0x7B8A9C);
    static final Color DISABLED_FILL = new Color(0xEEF2F6);

    // feedback: a dark text colour on a light tint of the same colour
    static final Color SUCCESS_TEXT = new Color(0x146C2E);
    static final Color SUCCESS_TINT = new Color(0xE3F5E8);
    static final Color WARNING_TEXT = new Color(0x8A5A00);
    static final Color WARNING_TINT = new Color(0xFFF4D6);
    static final Color DANGER_TEXT = new Color(0xB3261E);
    static final Color DANGER_TINT = new Color(0xFDECEA);
    static final Color DANGER_BORDER = new Color(0xE3B5B5);

    private Palette() {
    }
}
