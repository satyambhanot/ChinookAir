package ca.ucalgary.chinookair.presentation;

/** How a screen asks for another screen. The main frame implements it, so screens never know each other. */
public interface Navigator {

    void show(ViewId view);
}
