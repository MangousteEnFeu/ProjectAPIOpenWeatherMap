package ch.hearc.meteo.business;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une station météo dans le domaine métier.
 * Chaque station est associée à un pays et possède une liste de mesures {@link Meteo}.
 * Sert d’agrégat principal pour regrouper les données issues d'OpenWeatherMap.
 */
public class StationMeteo {
    private Integer numero;          // identifiant interne (optionnel)
    private String nom;              // nom de la station ou de la ville
    private Pays pays;               // pays associé à la station
    private Double latitude;         // coordonnées géographiques
    private Double longitude;
    private Integer openWeatherMapId; // identifiant OWM (utile pour traçabilité)
    private List<Meteo> donneesMeteo = new ArrayList<>(); // liste de mesures météo

    public StationMeteo() {}

    // Accesseurs standards (POJO)
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Pays getPays() { return pays; }
    public void setPays(Pays pays) { this.pays = pays; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getOpenWeatherMapId() { return openWeatherMapId; }
    public void setOpenWeatherMapId(Integer openWeatherMapId) { this.openWeatherMapId = openWeatherMapId; }

    public List<Meteo> getDonneesMeteo() { return donneesMeteo; }
    public void setDonneesMeteo(List<Meteo> donneesMeteo) { this.donneesMeteo = donneesMeteo; }

    /**
     * Ajoute une mesure à la liste si elle n'est pas nulle.
     * Simplifie l'ajout sans avoir à vérifier à chaque appel.
     */
    public void addMeteo(Meteo m) {
        if (m != null) this.donneesMeteo.add(m);
    }

    @Override
    public String toString() {
        // Affichage compact pour logs/debug : nombre de mesures plutôt que contenu complet
        return "StationMeteo{" +
                "numero=" + numero +
                ", nom='" + nom + '\'' +
                ", pays=" + (pays != null ? pays.getNom() : null) +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", openWeatherMapId=" + openWeatherMapId +
                ", donneesMeteoCount=" + (donneesMeteo != null ? donneesMeteo.size() : 0) +
                '}';
    }
}
