package ca.ucalgary.chinookair.presentation;

/** How a screen asks for another screen. The main frame implements it, so screens never know each other. */
interface Navigator {

    void show(ViewId view);

    /** Refreshes the navigation bar when the signed-in user's details change on the current screen. */
    default void refreshBar() {}
}
