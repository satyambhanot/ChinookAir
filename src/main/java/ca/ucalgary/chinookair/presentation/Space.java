package ca.ucalgary.chinookair.presentation;

/**
 * The few distances the screens are allowed to use, in pixels. Padding, gaps and insets are chosen
 * from this list instead of being typed as numbers, so two screens that mean the same gap use the
 * same one. A test fails the build if a screen writes its own number.
 */
final class Space {

    static final int XS = 4;
    static final int S = 8;
    static final int M = 12;
    static final int L = 16;
    static final int XL = 24;
    static final int XXL = 32;
    static final int XXXL = 40;

    /** The side of a page: the same on every screen, so headings start at the same place. */
    static final int PAGE = XXL;

    /**
     * The height of a field's label, used to line a button up with the fields beside it. A stop-gap
     * until the filter bars are rebuilt with one shared layout.
     */
    static final int LABEL_HEIGHT = 22;

    private Space() {
    }
}
