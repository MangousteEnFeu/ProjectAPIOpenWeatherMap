package ch.hearc.meteo.service;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.Pays;
import ch.hearc.meteo.business.StationMeteo;
import ch.hearc.meteo.exception.ApiClientException;
import ch.hearc.meteo.infrastructure.http.CountryClient;
import ch.hearc.meteo.infrastructure.http.OpenWeatherMapClient;
import ch.hearc.meteo.infrastructure.persistence.MeteoRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MeteoServiceImpl implements MeteoService {

    private final OpenWeatherMapClient owmClient;
    private final CountryClient countryClient;
    private final MeteoRepository meteoRepository;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    public MeteoServiceImpl(OpenWeatherMapClient owmClient,
                            CountryClient countryClient,
                            MeteoRepository meteoRepository) {
        if (owmClient == null) {
            throw new IllegalArgumentException("owmClient requis");
        }
        this.owmClient = owmClient;
        this.countryClient = countryClient;
        this.meteoRepository = meteoRepository;
    }

    @Override
    public StationMeteo obtenirMeteoEtTraiter(double latitude, double longitude, String langCountry) {
        // 1. Météo brute
        StationMeteo station = owmClient.fetchMeteo(null, null, latitude, longitude, langCountry);

        // 2. Enrichir pays (nom complet "Suisse")
        if (station != null && station.getPays() != null && station.getPays().getCode() != null && countryClient != null) {
            String code = station.getPays().getCode().trim();
            if (!code.isEmpty()) {
                try {
                    Pays p = countryClient.fetchPaysByAlpha2(code.toLowerCase(), (langCountry != null ? langCountry : "fr"));
                    if (p != null && p.getNom() != null && !p.getNom().isBlank()) {
                        station.setPays(p);
                    } else {
                        // fallback local
                        String nomLocal = new java.util.Locale("", code.toUpperCase())
                                .getDisplayCountry(langCountry != null ? new java.util.Locale(langCountry) : java.util.Locale.FRENCH);
                        if (nomLocal != null && !nomLocal.isBlank()) station.getPays().setNom(nomLocal);
                    }
                } catch (ApiClientException e) {
                    // fallback local
                    String nomLocal = new java.util.Locale("", code.toUpperCase())
                            .getDisplayCountry(langCountry != null ? new java.util.Locale(langCountry) : java.util.Locale.FRENCH);
                    if (nomLocal != null && !nomLocal.isBlank()) station.getPays().setNom(nomLocal);
                }
            }
        }

        // 3. Persistance éventuelle
        if (meteoRepository != null) {
            try {
                meteoRepository.save(station);
            } catch (Exception ex) {
                // on laisse passer l'erreur de DB, mais on ne plante pas l'affichage
                System.err.println("[WARN] Sauvegarde DB échouée: " + ex.getMessage());
            }
        }

        return station;
    }

    @Override
    public String toJsonResponse(Object obj) {
        return gson.toJson(obj);
    }

    @Override
    public List<String> listerStationsEnregistrees() {
        if (meteoRepository == null) {
            return Collections.emptyList();
        }
        try {
            return meteoRepository.findAllStationNames();
        } catch (Exception e) {
            System.err.println("[WARN] Lecture stations DB échouée: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Date> listerDatesPourStation(String stationName) {
        if (meteoRepository == null) {
            return Collections.emptyList();
        }
        try {
            return meteoRepository.findMeasurementDatesForStation(stationName);
        } catch (Exception e) {
            System.err.println("[WARN] Lecture dates DB échouée: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Meteo obtenirMeteoHistorique(String stationName, Date date) {
        if (meteoRepository == null) {
            return null;
        }
        try {
            return meteoRepository.findMeteoForStationAtDate(stationName, date);
        } catch (Exception e) {
            System.err.println("[WARN] Lecture relevé DB échouée: " + e.getMessage());
            return null;
        }
    }
}
