package ch.hearc.meteo.infrastructure.http;

import ch.hearc.meteo.business.Pays;
import ch.hearc.meteo.exception.ApiClientException;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client HTTP pour interroger le service externe "Country" à partir d’un code ISO (alpha2).
 * Convertit la réponse JSON en objet métier {@link Pays}.
 * Utilise HttpClient (Java 11+) et Gson pour le parsing.
 */
public class CountryClient {

    private final String baseUrl; // ex: https://db.ig.he-arc.ch/ens/scl/ws/country
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public CountryClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Appelle le service Country pour récupérer le nom du pays correspondant au code alpha2.
     * @param alpha2 code ISO 2 lettres (ex: "CH")
     * @param lang   code langue optionnel pour la traduction (ex: "fr")
     * @return objet {@link Pays} ou null si la réponse est vide
     * @throws ApiClientException si l'appel HTTP échoue ou si le code HTTP n’est pas 2xx
     */
    public Pays fetchPaysByAlpha2(String alpha2, String lang) {
        try {
            // Construit dynamiquement l’URL selon la présence du paramètre "lang"
            String url = baseUrl.endsWith("/") ? baseUrl + alpha2 : baseUrl + "/" + alpha2;
            if (lang != null && !lang.isBlank()) url += "?lang=" + lang;

            System.out.println("[DEBUG] CountryClient URL = " + url); // trace utile en dev

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new ApiClientException("Country API HTTP " + resp.statusCode() + " - " + resp.body());
            }

            System.out.println("[DEBUG] CountryClient response = " + resp.body()); // trace utile en debug

            // Désérialisation JSON → DTO simple
            CountryDto dto = gson.fromJson(resp.body(), CountryDto.class);
            if (dto == null) return null;

            // Conversion DTO → objet métier
            Pays p = new Pays();
            p.setCode(dto.code != null ? dto.code.toUpperCase() : null);
            p.setNom(dto.name); // correspondance champ JSON "name" → Pays.nom
            return p;

        } catch (IOException | InterruptedException e) {
            // Interruption rétablie pour ne pas masquer le statut du thread
            Thread.currentThread().interrupt();
            throw new ApiClientException("Country API error: " + e.getMessage(), e);
        }
    }

    /** Structure minimale pour mapper la réponse JSON du service Country */
    private static class CountryDto {
        String code;
        String name;
    }
}
