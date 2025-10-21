package ch.hearc.meteo.infrastructure.http;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.Pays;
import ch.hearc.meteo.business.StationMeteo;
import ch.hearc.meteo.dto.OpenWeatherMapResponse;
import ch.hearc.meteo.exception.ApiClientException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;

/**
 * Client HTTP pour l’API OpenWeatherMap (version 2.5 - Current Weather).
 * Interroge l’API par nom de ville ou coordonnées et convertit la réponse JSON
 * en objets métier {@link StationMeteo}, {@link Meteo}, {@link Pays}.
 */
public class OpenWeatherMapClient {

    private final String baseUrl; // ex: https://api.openweathermap.org/data/2.5/weather
    private final String apiKey;  // clé d’API personnelle
    private final HttpClient http;
    private final Gson gson;

    public OpenWeatherMapClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.http = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    /**
     * Récupère les données météo courantes pour une localisation donnée.
     * - Si city est fournie → requête par nom.
     * - Sinon → requête par latitude/longitude.
     * @param city nom de la ville (optionnel)
     * @param countryCode code ISO du pays (optionnel, pour affiner la recherche)
     * @param latitude latitude si city est null
     * @param longitude longitude si city est null
     * @param lang code langue pour la description (ex: "fr")
     * @return {@link StationMeteo} complète avec ses mesures
     * @throws ApiClientException en cas d’erreur HTTP ou d’appel invalide
     */
    public StationMeteo fetchMeteo(String city, String countryCode, double latitude, double longitude, String lang) {
        try {
            // Construction dynamique de l’URL selon la méthode choisie (ville ou coordonnées)
            StringBuilder url = new StringBuilder(baseUrl);
            if (city != null && !city.isBlank()) {
                // Exemple : q=London,uk
                url.append("?q=").append(URLEncoder.encode(city, StandardCharsets.UTF_8));
                if (countryCode != null && !countryCode.isBlank()) {
                    url.append(",").append(countryCode);
                }
            } else {
                url.append("?lat=").append(latitude).append("&lon=").append(longitude);
            }

            // Ajout des paramètres communs
            url.append("&appid=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
            url.append("&units=metric");
            if (lang != null && !lang.isBlank()) url.append("&lang=").append(lang);

            // Préparation et envoi de la requête HTTP GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            // Gestion d'erreur basique selon le code HTTP
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiClientException("OWM HTTP " + response.statusCode() + " : " + response.body());
            }

            // Désérialisation JSON → DTO
            OpenWeatherMapResponse dto = gson.fromJson(response.body(), OpenWeatherMapResponse.class);
            if (dto == null) throw new ApiClientException("Réponse vide de OpenWeatherMap");

            // Transformation DTO → modèle métier
            return mapDtoToStation(dto);

        } catch (IOException | InterruptedException e) {
            // Ré-interrompt le thread pour ne pas perdre l’état d’interruption
            Thread.currentThread().interrupt();
            throw new ApiClientException("Erreur appel OWM: " + e.getMessage(), e);
        }
    }

    /**
     * Convertit la réponse OpenWeatherMap (DTO) en objets métier exploitables.
     */
    private StationMeteo mapDtoToStation(OpenWeatherMapResponse dto) {
        StationMeteo station = new StationMeteo();
        station.setNom(dto.getName());

        if (dto.getCoord() != null) {
            station.setLatitude(dto.getCoord().getLat());
            station.setLongitude(dto.getCoord().getLon());
        }

        // Création du pays à partir du code ISO
        if (dto.getSys() != null && dto.getSys().getCountry() != null) {
            Pays pays = new Pays();
            pays.setCode(dto.getSys().getCountry());
            station.setPays(pays);
        }

        // Remplissage des données météo
        Meteo m = new Meteo();
        m.setDateMesure(new Date());
        if (dto.getMain() != null) {
            m.setTemperature(dto.getMain().getTemp());
            m.setPression(dto.getMain().getPressure());
            if (dto.getMain().getHumidity() != null)
                m.setHumidite(dto.getMain().getHumidity().doubleValue());
        }
        if (dto.getWeather() != null && !dto.getWeather().isEmpty()) {
            m.setDescription(dto.getWeather().get(0).getDescription());
        }
        if (dto.getVisibility() != null) m.setVisibilite(dto.getVisibility());
        if (dto.getRain() != null && dto.getRain().getH1() != null) {
            m.setPrecipitation(dto.getRain().getH1());
        }

        station.addMeteo(m);
        return station;
    }
}
