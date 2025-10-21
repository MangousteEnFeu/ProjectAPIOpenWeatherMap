package ch.hearc.meteo.business;

import java.util.Date;

/**
 * Représente une mesure météo unique (données brutes issues d'une station).
 * Contient uniquement les valeurs physiques + description textuelle.
 * ⚠️ La date est stockée en {@link java.util.Date} pour compatibilité, mais pourrait être remplacée
 *     par {@link java.time.Instant} ou {@link java.time.LocalDateTime} si besoin de précision/UTC.
 */
public class Meteo {
    // Identifiant logique ou numéro d'ordre de la mesure
    private Integer numero;

    // Date/heure de la mesure (instant de capture)
    private Date dateMesure;

    // Valeurs principales : peuvent être null si non fournies par l’API
    private Double temperature;
    private String description;
    private Double pression;
    private Double humidite;
    private Integer visibilite;   // en mètres
    private Double precipitation; // en millimètres

    public Meteo() {}

    // Getters / Setters classiques (POJO)
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public Date getDateMesure() { return dateMesure; }
    public void setDateMesure(Date dateMesure) { this.dateMesure = dateMesure; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPression() { return pression; }
    public void setPression(Double pression) { this.pression = pression; }

    public Double getHumidite() { return humidite; }
    public void setHumidite(Double humidite) { this.humidite = humidite; }

    public Integer getVisibilite() { return visibilite; }
    public void setVisibilite(Integer visibilite) { this.visibilite = visibilite; }

    public Double getPrecipitation() { return precipitation; }
    public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }

    @Override
    public String toString() {
        // Utile pour le debug et le logging
        return "Meteo{" +
                "dateMesure=" + dateMesure +
                ", temperature=" + temperature +
                ", description='" + description + '\'' +
                ", pression=" + pression +
                ", humidite=" + humidite +
                ", visibilite=" + visibilite +
                ", precipitation=" + precipitation +
                '}';
    }
}
