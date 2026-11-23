package de.witchcafe.quiz.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;

import de.witchcafe.ui.Responsive;

/**
 * Grids auf dem Handy kompakt: niedrigere Zeilen und Text umbrechen statt mit "..."
 * abschneiden. Am Desktop bleibt das Grid wie es ist.
 */
final class MobilGrid {

    private MobilGrid() {}

    static void kompakt(Grid<?> grid) {
        Responsive.onMobileChange(grid, mobil -> setze(grid, mobil));
    }

    static void setze(Grid<?> grid, boolean mobil) {
        if (mobil) {
            grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        } else {
            grid.removeThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        }
    }
}
