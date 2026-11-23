# Quiz API – Anleitung (Version {{version}})

Mit der Quiz API liest du die **Fragen** und **Kategorien** des Quiz und importierst neue **Fragenkataloge**.
Quiz spielen und Ergebnisse ansehen geht nur in der Oberfläche.

## Kategorien: Baum aus Pfaden

Kategorien sind hierarchisch. Die Ebenen stehen im Namen, getrennt durch `/`:

```
Drohnen                                  Oberthema
Drohnen/EU A1-A3                         Zertifikat
Drohnen/EU A1-A3/Luftrecht               Abschnitt
```

Eine Frage gehört zu genau einer Kategorie (Feld `thema`), meist zur untersten Ebene. Oberthemen entstehen
automatisch aus den Pfaden. Ein Quiz und `GET /api/fragen?kategorie=…` auf einer oberen Ebene nehmen die
Fragen aller Unterbereiche mit. Zu jeder Kategorie können Editoren in der Oberfläche (Kategorien) **Schlagworte**
vergeben, nach denen Benutzer suchen können; dort lassen sich Kategorien auch umbenennen und verschieben.

## API-Key

Alle Endpunkte unter `/api/**` außer `/api/docs/**` brauchen den Header

```
Authorization: Bearer <API-Key>
```

Einen Key legt ein Admin in der Oberfläche unter **Admin → API-Keys** (`/admin/apikeys`) an. Der Key beginnt
mit `qz_` und wird nur einmal angezeigt. Seine Rolle bestimmt, was er darf:

| Rolle  | darf                                  |
|--------|---------------------------------------|
| USER   | Kategorien und Fragen lesen           |
| EDITOR | lesen und Fragenkataloge importieren  |
| ADMIN  | wie EDITOR                            |

In den Beispielen steht `$QUIZ` für die Adresse der App (z. B. `https://quiz.segieth.cologne`) und `$KEY` für den API-Key.

## Kategorien lesen

```
curl -H "Authorization: Bearer $KEY" $QUIZ/api/kategorien
```

Jeder Knoten des Baums, auch Oberthemen ohne eigene Fragen, Eltern vor Kindern:

```json
[
  {"name": "Drohnen", "oberkategorie": null, "anzahl": 0, "anzahlGesamt": 120,
   "schlagworte": "UAS, Multicopter"},
  {"name": "Drohnen/EU A1-A3", "oberkategorie": "Drohnen", "anzahl": 0, "anzahlGesamt": 120,
   "schlagworte": "Kompetenznachweis, Open Category"},
  {"name": "Drohnen/EU A1-A3/Luftrecht", "oberkategorie": "Drohnen/EU A1-A3", "anzahl": 40, "anzahlGesamt": 40,
   "schlagworte": ""}
]
```

`name` ist der volle Pfad (so wie `thema` beim Import), `anzahl` zählt die Fragen genau dieser Kategorie,
`anzahlGesamt` inklusive aller Unterbereiche.

## Fragen lesen

Alle Fragen oder nur die einer Kategorie samt ihrer Unterbereiche (Pfad wie in `/api/kategorien`, URL-kodiert):

```
curl -H "Authorization: Bearer $KEY" "$QUIZ/api/fragen?kategorie=Geografie%2FDeutschland"
```

```json
[{
  "id": 17,
  "thema": "Geografie/Deutschland/Flüsse",
  "frage": "Welcher Fluss fließt durch Köln?",
  "antworten": ["Main", "Rhein", "Elbe", "Mosel"],
  "correct_answer": "Rhein",
  "beschreibung": "Köln liegt am Rhein."
}]
```

Die Antwort enthält die richtige Antwort, gib den Key also nicht an Spieler weiter.

## Format der Upload-Dateien (Fragenkatalog)

Ein Fragenkatalog ist eine **JSON-Datei mit einem Array von Fragen**, kodiert in **UTF-8**, höchstens **10 MB**.
Dasselbe Format gilt für den Upload in der Oberfläche (Fragen → Upload) und für `POST /api/fragen`.
Der Dateiname ist egal, üblich ist `*.json`.

Jede Frage ist ein Objekt mit diesen Feldern:

| Feld             | Typ             | Pflicht | Bedeutung                                                    |
|------------------|-----------------|---------|--------------------------------------------------------------|
| `thema`          | Text            | ja      | Kategorie als Pfad, Ebenen mit `/` getrennt (z. B. `Drohnen/EU A1-A3/Luftrecht`); neue Kategorien und Oberthemen entstehen automatisch |
| `frage`          | Text            | ja      | Fragetext                                                    |
| `antworten`      | Liste von Texten| nein    | alle Antwortmöglichkeiten, **die richtige eingeschlossen**; Reihenfolge beliebig |
| `correct_answer` | Text            | ja      | die richtige Antwort, **genau derselbe Text** wie in `antworten` |
| `beschreibung`   | Text            | nein    | Erklärung zur Antwort, höchstens 300 Zeichen                  |

Regeln:

- Die Feldnamen sind klein geschrieben, `correct_answer` mit Unterstrich.
- Leerzeichen am Anfang und Ende von `frage` und `correct_answer` werden entfernt, in `thema` auch um jedes `/`
  (`Drohnen / EU A1-A3` wird zu `Drohnen/EU A1-A3`). Groß-/Kleinschreibung zählt: `drohnen` ist eine andere
  Kategorie als `Drohnen`.
- Schlagworte gehören zur Kategorie, nicht zur Frage, und werden in der Oberfläche gepflegt, nicht in der Datei.
- Weitere Felder (z. B. `id` aus einem Export, eigene Notizen) werden ignoriert. Ein Export aus
  `GET /api/fragen` lässt sich also direkt wieder hochladen, legt dann aber **neue** Fragen an (Dubletten).
- Eine Datei darf Fragen mehrerer Kategorien enthalten.
- Die Datei muss ein Array sein (`[ … ]`), auch bei nur einer Frage.

Beispiel `fragen.json`:

```json
[
  {
    "thema": "Geografie/Deutschland/Flüsse",
    "frage": "Welcher Fluss fließt durch Köln?",
    "antworten": ["Main", "Rhein", "Elbe", "Mosel"],
    "correct_answer": "Rhein",
    "beschreibung": "Köln liegt am Rhein."
  },
  {
    "thema": "Geografie/Europa/Hauptstädte",
    "frage": "Wie heißt die Hauptstadt von Österreich?",
    "antworten": ["Wien", "Graz", "Salzburg"],
    "correct_answer": "Wien"
  }
]
```

## Fragenkatalog importieren (Rolle EDITOR oder ADMIN)

Als JSON-Body:

```
curl -X POST -H "Authorization: Bearer $KEY" -H "Content-Type: application/json; charset=utf-8" \
     --data-binary @fragen.json $QUIZ/api/fragen
```

Oder als Datei-Upload (Feld `file`):

```
curl -X POST -H "Authorization: Bearer $KEY" -F "file=@fragen.json" $QUIZ/api/fragen
```

Antwort:

```json
{"angelegt": 9, "fehler": ["Eintrag 4: thema, frage und correct_answer sind Pflicht"]}
```

Fehlerhafte Einträge werden übersprungen und in `fehler` gemeldet (mit ihrer Position im Array), der Rest wird angelegt.
Der Import prüft **nicht** auf Dubletten: sieh vorher mit `GET /api/fragen?kategorie=…` nach, was schon da ist.

## Fehler

| Status | Bedeutung                                                         |
|--------|-------------------------------------------------------------------|
| 400    | ungültiges JSON, Antwort `{"fehler": "…"}`                         |
| 401    | API-Key fehlt oder ist ungültig                                   |
| 403    | Rolle reicht nicht (Import nur EDITOR/ADMIN), Antwort `{"fehler": "…"}` |

## Tipps

- Texte immer als UTF-8 senden. Mit curl unter Windows/Git Bash `--data-binary @datei.json` statt `-d '…'`
  nehmen, sonst gehen Umlaute kaputt.
- `GET /api/docs` liefert die aktuelle `version`. Wer die API automatisiert nutzt, merkt sich die Version und liest
  bei einer neuen `GET /api/docs/aenderungen`.
- Die vollständige, aus dem Code erzeugte Beschreibung steht in `GET /api/docs/openapi.json`.
