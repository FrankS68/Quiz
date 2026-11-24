package de.witchcafe.quiz.ui;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

import de.witchcafe.quiz.Kategorie;
import de.witchcafe.quiz.KategorieService.Info;

/** Fuellt ein TreeGrid mit dem Kategorienbaum, optional gefiltert nach einem Suchtext. */
final class KategorieBaum {

    private KategorieBaum() {}

    /**
     * Zeigt alle Knoten, deren Pfad oder Schlagworte den Suchtext enthalten, dazu ihre
     * Oberthemen (damit der Baum zusammenhaengt) und ihre Unterkategorien.
     * Ohne Suchtext: alles, Oberthemen der ersten Ebene aufgeklappt; mit Suchtext: alles aufgeklappt.
     */
    static void fuelle(TreeGrid<Info> grid, List<Info> alle, String suche) {
        String s = suche == null ? "" : suche.trim().toLowerCase(Locale.ROOT);
        Set<String> sichtbar = new HashSet<>();
        if (s.isEmpty()) {
            alle.forEach(i -> sichtbar.add(i.pfad()));
        } else {
            List<String> treffer = alle.stream()
                    .filter(i -> i.pfad().toLowerCase(Locale.ROOT).contains(s)
                            || i.schlagworte().toLowerCase(Locale.ROOT).contains(s))
                    .map(Info::pfad)
                    .toList();
            for (Info i : alle) {
                for (String t : treffer) {
                    // Treffer selbst, seine Unterkategorien und seine Oberthemen
                    if (Kategorie.imTeilbaum(i.pfad(), t) || Kategorie.imTeilbaum(t, i.pfad())) {
                        sichtbar.add(i.pfad());
                        break;
                    }
                }
            }
        }

        TreeData<Info> daten = new TreeData<>();
        Map<String, Info> nachPfad = new LinkedHashMap<>();
        for (Info i : alle) { // sortiert: Eltern vor Kindern
            if (!sichtbar.contains(i.pfad())) {
                continue;
            }
            Info eltern = i.eltern() == null ? null : nachPfad.get(i.eltern());
            daten.addItem(eltern, i);
            nachPfad.put(i.pfad(), i);
        }
        grid.setDataProvider(new TreeDataProvider<>(daten));
        if (s.isEmpty()) {
            grid.expand(daten.getRootItems());
        } else {
            grid.expandRecursively(daten.getRootItems(), Integer.MAX_VALUE);
        }
    }
}
