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
import java.util.Locale;

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
        // 1. Appel API OpenWeather
        StationMeteo station = owmClient.fetchMeteo(null, null, latitude, longitude, langCountry);

        // 2. Enrichir le pays (nom lisible, ex: "Suisse")
        if (station != null
                && station.getPays() != null
                && station.getPays().getCode() != null
                && countryClient != null) {

            String code = station.getPays().getCode().trim();
            if (!code.isEmpty()) {
                try {
                    Pays p = countryClient.fetchPaysByAlpha2(
                            code.toLowerCase(),
                            (langCountry != null ? langCountry : "fr")
                    );
                    if (p != null && p.getNom() != null && !p.getNom().isBlank()) {
                        station.setPays(p);
                    } else {
                        // fallback via Locale Java si l'API pays ne renvoie pas de nom
                        String nomLocal = new Locale("", code.toUpperCase())
                                .getDisplayCountry(langCountry != null ? new Locale(langCountry) : Locale.FRENCH);
                        if (nomLocal != null && !nomLocal.isBlank()) {
                            station.getPays().setNom(nomLocal);
                        }
                    }
                } catch (ApiClientException e) {
                    // fallback si l'API pays plante
                    String nomLocal = new Locale("", code.toUpperCase())
                            .getDisplayCountry(langCountry != null ? new Locale(langCountry) : Locale.FRENCH);
                    if (nomLocal != null && !nomLocal.isBlank()) {
                        station.getPays().setNom(nomLocal);
                    }
                }
            }
        }

        // 3. Sauvegarde en DB (si repo dispo)
        if (meteoRepository != null) {
            try {
                meteoRepository.save(station);
            } catch (Exception ex) {
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

    @Override
    public StationMeteo capturerMeteoPourStationEnregistree(String stationName, String langCountry) {
        if (meteoRepository == null) {
            System.err.println("[WARN] Pas de base de données configurée, impossible d'utiliser une station enregistrée.");
            return null;
        }
        try {
            // 1. On va chercher les coordonnées existantes de la station
            StationMeteo existante = meteoRepository.findStationByName(stationName);
            if (existante == null) {
                System.err.println("[WARN] Station inconnue : " + stationName);
                return null;
            }

            Double lat = existante.getLatitude();
            Double lon = existante.getLongitude();
            if (lat == null || lon == null) {
                System.err.println("[WARN] La station " + stationName + " n'a pas de coordonnées en base.");
                return null;
            }

            // 2. On réutilise la même logique que obtenirMeteoEtTraiter :
            //    - appel API météo actuelle avec ces coordonnées
            //    - enrichissement pays
            //    - insertion du relevé en base
            return obtenirMeteoEtTraiter(lat, lon, langCountry);

        } catch (Exception e) {
            System.err.println("[WARN] Impossible de récupérer la station '" + stationName + "': " + e.getMessage());
            return null;
        }
    }
}
