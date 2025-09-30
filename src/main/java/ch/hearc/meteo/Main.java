package ch.hearc.meteo;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Lancement de l'application Météo HE-Arc...");

        // Logique principale à implémenter ici
        // 1. Saisir les coordonnées
        Scanner scanner = new Scanner(System.in);
        System.out.print("Entrez la latitude : ");
        double latitude = scanner.nextDouble();
        System.out.print("Entrez la longitude : ");
        double longitude = scanner.nextDouble();

        System.out.println("Coordonnées saisies : " + latitude + ", " + longitude);

        // 2. Interroger le service web (à faire)
        // 3. Désérialiser la réponse (à faire)
        // 4. Persister les objets en base de données (à faire)

        System.out.println("Application terminée.");
    }
}