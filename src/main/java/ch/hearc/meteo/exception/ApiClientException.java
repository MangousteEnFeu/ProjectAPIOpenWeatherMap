package ch.hearc.meteo.exception;

/**
 * Exception métier levée lorsqu’un appel HTTP vers une API externe échoue
 * (erreur réseau, code HTTP inattendu, parsing JSON, etc.).
 * Hérite de RuntimeException pour simplifier la propagation sans déclaration explicite.
 */
public class ApiClientException extends RuntimeException {

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
