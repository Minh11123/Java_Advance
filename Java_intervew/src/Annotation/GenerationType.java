package Annotation;

/**
 * Strategies for primary key generation in the simulated JPA layer.
 */
public enum GenerationType {
    AUTO,
    IDENTITY,
    SEQUENCE,
    TABLE
}
