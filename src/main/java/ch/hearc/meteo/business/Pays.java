package ch.hearc.meteo.business;

/**
 * Représente un pays dans le modèle métier.
 * Utilisé notamment pour relier une station météo à son pays d'appartenance.
 * Les champs correspondent généralement à ceux renvoyés par le service "Country".
 */
public class Pays {
    private Integer numero; // identifiant interne (optionnel)
    private String code;    // code ISO alpha2, ex: "CH"
    private String nom;     // nom du pays, ex: "Suisse"

    public Pays() {}

    // Accesseurs standards (POJO)
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    @Override
    public String toString() {
        // Retourne une chaîne lisible pour le debug et les logs
        return "Pays{" + "code='" + code + '\'' + ", nom='" + nom + '\'' + '}';
    }
}
