package ca.ucalgary.chinookair.presentation;

import java.util.ArrayList;
import java.util.List;

/** A navigator for tests: it only remembers which screens were asked for. */
final class RecordingNavigator implements Navigator {

    final List<ViewId> shown = new ArrayList<>();

    @Override
    public void show(ViewId view) {
        shown.add(view);
    }

    ViewId last() {
        return shown.isEmpty() ? null : shown.get(shown.size() - 1);
    }
}
