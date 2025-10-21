package ch.hearc.meteo.infrastructure.persistence;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;

/**
 * Interface de persistance du modèle métier météo.
 * Sert de contrat entre la couche service et la couche d’accès aux données.
 * Une implémentation concrète (ex. OracleMeteoRepository) gère la base réelle.
 * → Permet d’injecter un mock pour les tests sans dépendre d’une BDD.
 */
public interface MeteoRepository {

    /**
     * Persiste une station et ses mesures associées.
     * L’implémentation doit s’occuper de la gestion transactionnelle.
     * @param station la station complète à sauvegarder (avec ses mesures)
     * @throws Exception en cas d’erreur d’accès ou de transaction
     */
    void save(StationMeteo station) throws Exception;
}
