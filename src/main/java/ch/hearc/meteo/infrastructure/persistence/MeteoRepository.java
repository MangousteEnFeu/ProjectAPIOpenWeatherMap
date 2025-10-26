package ch.hearc.meteo.infrastructure.persistence;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;

import java.util.Date;
import java.util.List;
/**
 * Interface d'accès aux données persistées (Oracle).
 *
 * Steven implémente tout ça dans OracleMeteoRepository.
 */
public interface MeteoRepository {

    /**
     * Sauvegarde en base :
     * - pays (si pas déjà présent)
     * - station (si pas déjà présente)
     * - mesure météo associée
     */
    void save(StationMeteo station) throws Exception;

    /**
     * Retourne les noms des stations météo existantes
     * triées par ordre alphabétique.
     *
     * Exemple: ["Genève", "HE-Arc Neuchâtel", "Paris"]
     */
    List<String> findAllStationNames() throws Exception;

    /**
     * Retourne la liste des dates de relevé météo
     * pour une station donnée (par son nom exact).
     *
     * Ex: [2025-10-26 21:05:00, 2025-10-27 08:12:00, ...]
     */
    List<Date> findMeasurementDatesForStation(String stationName) throws Exception;

    /**
     * Retourne le relevé météo complet (température, pression,
     * humidité, etc.) pour une station et une date donnée.
     *
     * Si plusieurs relevés ont la même timestamp en base,
     * on renvoie le plus proche / premier.
     */
    Meteo findMeteoForStationAtDate(String stationName, Date date) throws Exception;
}
