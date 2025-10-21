package ch.hearc.meteo.service;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;

/**
 * Interface du service applicatif principal.
 * Coordonne les appels aux clients externes (API météo + pays) et la persistance éventuelle.
 * Sert d’abstraction pour isoler la logique métier du reste de l’application.
 */
public interface MeteoService {

    /**
     * Récupère les données météo à partir d’une position géographique,
     * résout le pays correspondant et persiste les résultats si un repository est disponible.
     *
     * @param latitude    latitude décimale
     * @param longitude   longitude décimale
     * @param langCountry code langue utilisé pour le nom du pays (ex: "fr", "en")
     * @return une {@link StationMeteo} complète avec sa ou ses mesures météo
     */
    StationMeteo obtenirMeteoEtTraiter(double latitude, double longitude, String langCountry);

    /**
     * Sérialise un objet métier (ex: {@link StationMeteo} ou {@link Meteo}) en JSON.
     * Utilisé pour exposer les données sous forme lisible ou transmissible.
     *
     * @param obj instance à sérialiser
     * @return représentation JSON sous forme de chaîne
     */
    String toJsonResponse(Object obj);
}
