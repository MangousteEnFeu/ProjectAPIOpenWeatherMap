package ch.hearc.meteo.service;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;

import java.util.Date;
import java.util.List;

public interface MeteoService {

    /**
     * Récupère la météo actuelle pour lat/lon, enrichit le pays,
     * et (si un repository est présent) persiste en base.
     */
    StationMeteo obtenirMeteoEtTraiter(double latitude, double longitude, String langCountry);

    /**
     * Sérialise en JSON pour debug / réutilisation API.
     */
    String toJsonResponse(Object obj);

    /**
     * Renvoie les noms des stations enregistrées (triés).
     * Retourne liste vide si pas de repo.
     */
    List<String> listerStationsEnregistrees();

    /**
     * Renvoie les dates des relevés pour une station.
     */
    List<Date> listerDatesPourStation(String stationName);

    /**
     * Renvoie les infos météo détaillées
     * pour une station à une date précise.
     */
    Meteo obtenirMeteoHistorique(String stationName, Date date);
}
