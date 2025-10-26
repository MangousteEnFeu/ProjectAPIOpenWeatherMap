package ch.hearc.meteo.service;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;

import java.util.Date;
import java.util.List;

public interface MeteoService {

    /**
     * Va sur l'API météo pour une latitude/longitude,
     * enrichit le pays, sauvegarde si la DB est dispo,
     * et renvoie la station météo + relevé courant.
     */
    StationMeteo obtenirMeteoEtTraiter(double latitude, double longitude, String langCountry);

    /**
     * Sérialisation JSON (debug / affichage brut si besoin).
     */
    String toJsonResponse(Object obj);

    /**
     * Liste alphabétique des stations en base (pour les menus).
     */
    List<String> listerStationsEnregistrees();

    /**
     * Liste des dates de relevés pour une station donnée,
     * triées du plus récent au plus ancien.
     */
    List<Date> listerDatesPourStation(String stationName);

    /**
     * Récupère un relevé historique précis.
     */
    Meteo obtenirMeteoHistorique(String stationName, Date date);

    /**
     * Récupère les coordonnées d'une station déjà enregistrée
     * (via la DB), interroge l'API météo ACTUELLE pour cette
     * station, enregistre le nouveau relevé en base, et renvoie
     * le résultat comme pour obtenirMeteoEtTraiter.
     */
    StationMeteo capturerMeteoPourStationEnregistree(String stationName, String langCountry);
}
