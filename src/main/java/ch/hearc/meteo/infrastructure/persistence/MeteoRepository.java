package ch.hearc.meteo.infrastructure.persistence;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;

import java.util.Date;
import java.util.List;

/**
 * Accès à la base Oracle (lecture/écriture).
 * Implémenté par OracleMeteoRepository.
 */
public interface MeteoRepository {

    /**
     * Sauvegarde en base :
     * - le pays (si pas déjà présent)
     * - la station (si pas déjà présente)
     * - le relevé météo associé
     */
    void save(StationMeteo station) throws Exception;

    /**
     * Retourne les noms des stations météo connues (ordre alphabétique).
     */
    List<String> findAllStationNames() throws Exception;

    /**
     * Liste les timestamps de relevés météo pour une station, triés
     * (le plus récent en premier).
     */
    List<Date> findMeasurementDatesForStation(String stationName) throws Exception;

    /**
     * Retourne un relevé météo précis (température, etc.)
     * pour une station à une date donnée (par seconde).
     */
    Meteo findMeteoForStationAtDate(String stationName, Date date) throws Exception;

    /**
     * Retourne une station existante par son nom :
     * au moins nom / latitude / longitude / pays / openweather_id.
     * Sert pour re-capturer la météo actuelle à cet endroit.
     */
    StationMeteo findStationByName(String stationName) throws Exception;
}
