package cl.duoc.barriodigital.report.service;

/**
 * Payload que nunca se va a poder guardar (falta un campo obligatorio). Reintentarlo no
 * sirve: KafkaErrorConfig lo marca como no reintentable y va directo a la DLT.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
