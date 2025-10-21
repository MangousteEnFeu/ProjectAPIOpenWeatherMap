package ch.hearc.meteo.service;

import ch.hearc.meteo.business.Pays;
import ch.hearc.meteo.business.StationMeteo;
import ch.hearc.meteo.exception.ApiClientException;
import ch.hearc.meteo.infrastructure.http.CountryClient;
import ch.hearc.meteo.infrastructure.http.OpenWeatherMapClient;
import ch.hearc.meteo.infrastructure.persistence.MeteoRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implémentation principale du service météo.
 * Coordonne :
 *  - la récupération des données via OpenWeatherMapClient
 *  - l’enrichissement du pays via CountryClient (ou fallback local)
 *  - la sauvegarde éventuelle dans la base via MeteoRepository
 */
public class MeteoServiceImpl implements MeteoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteoServiceImpl.class);

    private final OpenWeatherMapClient owmClient;
    private final CountryClient countryClient;
    private final MeteoRepository meteoRepository;
    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    public MeteoServiceImpl(OpenWeatherMapClient owmClient,
                            CountryClient countryClient,
                            MeteoRepository meteoRepository) {
        if (owmClient == null) throw new IllegalArgumentException("owmClient requis");
        this.owmClient = owmClient;
        this.countryClient = countryClient;
        this.meteoRepository = meteoRepository;
    }

    /**
     * Récupère la météo via OpenWeatherMap, complète le pays (via API ou fallback locale),
     * puis persiste le résultat si un repository est disponible.
     */
    @Override
    public StationMeteo obtenirMeteoEtTraiter(double latitude, double longitude, String langCountry) {
        // Récupération des données météo
        StationMeteo station = owmClient.fetchMeteo(null, null, latitude, longitude, langCountry);

        // Enrichissement du nom du pays si code présent
        if (station != null && station.getPays() != null && station.getPays().getCode() != null && countryClient != null) {
            String code = station.getPays().getCode().trim();
            if (!code.isEmpty()) {
                try {
                    // Appel de l’API "Country" pour obtenir le nom traduit
                    Pays p = countryClient.fetchPaysByAlpha2(code.toLowerCase(),
                            (langCountry != null ? langCountry : "fr"));

                    if (p != null && p.getNom() != null && !p.getNom().isBlank()) {
                        station.setPays(p);
                    } else {
                        // Fallback : traduction locale via Locale
                        String nomLocal = new java.util.Locale("", code.toUpperCase())
                                .getDisplayCountry(langCountry != null
                                        ? new java.util.Locale(langCountry)
                                        : java.util.Locale.FRENCH);
                        if (nomLocal != null && !nomLocal.isBlank())
                            station.getPays().setNom(nomLocal);
                    }
                } catch (ApiClientException e) {
                    // En cas d’erreur HTTP → fallback local uniquement
                    String nomLocal = new java.util.Locale("", code.toUpperCase())
                            .getDisplayCountry(langCountry != null
                                    ? new java.util.Locale(langCountry)
                                    : java.util.Locale.FRENCH);
                    if (nomLocal != null && !nomLocal.isBlank())
                        station.getPays().setNom(nomLocal);
                }
            }
        }

        // Persistance optionnelle (si le repo a été injecté)
        if (meteoRepository != null) {
            try {
                meteoRepository.save(station);
            } catch (Exception ignore) {
                // Erreur silencieuse volontaire : persistance non bloquante
            }
        }

        return station;
    }

    /**
     * Sérialise un objet métier (StationMeteo, Meteo, etc.) en JSON formaté.
     */
    @Override
    public String toJsonResponse(Object obj) {
        if (obj == null) throw new IllegalArgumentException("obj null");
        return gson.toJson(obj);
    }
}
