# My Application README

- [ ] TODO Replace or update this README with instructions relevant to your application

## Project Structure

This project has the following structure:

```
src
├── main/java
│   └── [application package]
│       ├── base
│       │   └── ui
│       │       ├── ViewToolbar.java
│       │       └── MainLayout.java
│       ├── examplefeature
│       │   ├── ui
│       │   │   └── TaskListView.java
│       │   ├── Task.java
│       │   ├── TaskRepository.java
│       │   └── TaskService.java                
│       └── Application.java     
├── main/resources
│   ├── META-INF
│   │   └── resources
│   │       └── styles.css
│   └── application.properties 
└── test/java
    └── [application package]
        └── examplefeature
           └── TaskServiceTest.java                 
```

The main entry point into the application is `Application.java`. This class contains the `main()` method that starts up 
the Spring Boot application.

The project follows a *feature-based package structure*, organizing code by *functional units* rather than traditional 
architectural layers. It includes two feature packages: `base` and `examplefeature`.

* The `base` package contains classes meant for reuse across different features, either through composition or 
  inheritance. You can use them as-is, tweak them to your needs, or remove them.
* The `examplefeature` package is an example feature package that demonstrates the structure. It represents a 
  *self-contained unit of functionality*, including UI components, business logic, data access, and an integration test.
  Once you create your own features, *you'll remove this package*.


## Starting in Development Mode

To start the application in development mode, import it into your IDE and run the `Application` class. 
You can also start the application from the command line by running: 

```bash
./mvnw
```

## Building for Production

To build the application in production mode, run:

```bash
./mvnw package
```

To build a Docker image, run:

```bash
docker build -t my-application:latest .
```

If you use commercial components, pass the license key as a build secret:

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```

## Guthaben (witch-billing)

Das Quiz bucht über den zentralen Billing-Dienst ([witch-billing](https://github.com/FrankS68/witch-billing)),
Code in `QuizBilling`. Ohne `witch-billing.url` ist Billing aus und alles frei.

| Feature (App `quiz`) | Art | Wann | Startpreis |
|---|---|---|---|
| `spiel` | Verbrauch | Start einer Quiz-Runde | 0 (nur zählen) |
| `frage-angelegt` | Beitrag | Frage im Dialog angelegt | 5 |
| `frage-importiert` | Beitrag | pro Frage aus einem hochgeladenen Fragenkatalog | 1 |

- Reicht das Guthaben nicht, startet die Runde nicht (Hinweis mit Kontostand).
- Gutschriften erst nach erfolgreichem Speichern; Importe über die REST-API bekommen keine (kein Benutzer).
- Ist der Dienst nicht erreichbar, läuft das Quiz weiter (`witch-billing.fail-open=true`).
- Der Kontostand steht als Badge in der Kopfzeile der Quiz-Liste.

Einrichten:
1. Im Billing-Dienst unter *Preisliste* die drei Features oben anlegen.
2. API-Key erzeugen (siehe README von witch-billing): Hash als `BILLING_KEY_HASH_QUIZ` in den Dienst,
   Klartext als `WITCH_BILLING_API_KEY` ins Quiz, dazu `WITCH_BILLING_URL` (IONOS: `http://127.0.0.1:8085`).

## Next Steps

The [Building Apps](https://vaadin.com/docs/v25/building-apps) guides contain hands-on advice for adding features to 
your application.
