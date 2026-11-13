package ca.ucalgary.chinookair.presentation;

/** A screen that wants to know when it is shown, so it can clear its fields or reload what it displays. */
interface Screen {

    void onShow();
}
