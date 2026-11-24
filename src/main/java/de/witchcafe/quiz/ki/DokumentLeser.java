package de.witchcafe.quiz.ki;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

import org.jsoup.Jsoup;

/**
 * Laedt verlinkte Dokumente auf dem Server, fuer Anbieter ohne eigenes Web-Fetch.
 * Nur oeffentliche http(s)-Adressen: interne Netze, localhost usw. werden abgelehnt,
 * damit der Dialog nicht als Tuer ins interne Netz dient. Weiterleitungen werden einzeln geprueft.
 */
public final class DokumentLeser {

    /** Hoechstens so viele Zeichen Text pro Dokument an die KI (ca. 25.000 Tokens). */
    public static final int MAX_ZEICHEN = 100_000;
    /** Hoechstens so gross darf ein PDF sein. */
    public static final int MAX_PDF_BYTES = 20 * 1024 * 1024;

    private DokumentLeser() {
    }

    /** Text oder PDF-Inhalt eines Links; bei Problemen nur {@code hinweis}. */
    public record Dokument(String url, String text, byte[] pdf, String hinweis) {
        public boolean istPdf() {
            return pdf != null;
        }
    }

    /** Liest alle Links; Hinweise (gekuerzt, nicht lesbar) landen in {@code hinweise}. */
    public static List<Dokument> lies(List<String> urls, List<String> hinweise) {
        List<Dokument> docs = urls.stream().map(DokumentLeser::lies).toList();
        docs.stream().map(Dokument::hinweis).filter(h -> h != null).forEach(hinweise::add);
        return docs.stream().filter(d -> d.text() != null || d.istPdf()).toList();
    }

    public static Dokument lies(String url) {
        try {
            String aktuell = url.trim();
            for (int sprung = 0; sprung < 5; sprung++) {
                pruefeOeffentlich(aktuell);
                var antwort = Jsoup.connect(aktuell).userAgent("Quiz-KI-Fragengenerator").timeout(30_000)
                        .maxBodySize(MAX_PDF_BYTES).ignoreContentType(true).ignoreHttpErrors(true)
                        .followRedirects(false).execute();
                if (antwort.statusCode() / 100 == 3 && antwort.header("Location") != null) {
                    aktuell = URI.create(aktuell).resolve(antwort.header("Location")).toString();
                    continue;
                }
                if (antwort.statusCode() != 200) {
                    return new Dokument(url, null, null, url + " nicht lesbar: HTTP " + antwort.statusCode());
                }
                String typ = antwort.contentType() == null ? "" : antwort.contentType().toLowerCase();
                if (typ.contains("pdf")) {
                    return new Dokument(url, null, antwort.bodyAsBytes(), null);
                }
                String text = typ.contains("html") ? antwort.parse().text() : antwort.body();
                String hinweis = null;
                if (text.length() > MAX_ZEICHEN) {
                    text = text.substring(0, MAX_ZEICHEN);
                    hinweis = url + ": nur die ersten " + MAX_ZEICHEN + " Zeichen verwendet";
                }
                return new Dokument(url, text, null, hinweis);
            }
            return new Dokument(url, null, null, url + " nicht lesbar: zu viele Weiterleitungen");
        } catch (IOException | IllegalArgumentException e) {
            return new Dokument(url, null, null, url + " nicht lesbar: " + e.getMessage());
        }
    }

    static void pruefeOeffentlich(String url) throws UnknownHostException {
        URI uri = URI.create(url);
        String schema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!schema.equals("http") && !schema.equals("https") || uri.getHost() == null) {
            throw new IllegalArgumentException("nur http(s)-Adressen");
        }
        for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
            if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                    || a.isAnyLocalAddress() || a.isMulticastAddress()
                    || (a.getAddress().length == 16 && (a.getAddress()[0] & 0xfe) == 0xfc)) {
                throw new IllegalArgumentException("interne Adresse");
            }
        }
    }

    /** Text-Dokumente als Prompt-Anhang. */
    public static String alsText(List<Dokument> docs) {
        StringBuilder sb = new StringBuilder();
        for (Dokument d : docs) {
            if (d.text() != null) {
                sb.append("\n\n--- Inhalt von ").append(d.url()).append(" ---\n").append(d.text());
            }
        }
        return sb.toString();
    }
}
