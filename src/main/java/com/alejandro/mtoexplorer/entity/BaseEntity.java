package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * La base de todas las entidades: solo el id. Las consultas no viven aquí sino en los
 * repositorios ({@code repository/}), que es lo que separa los datos de cómo se buscan.
 *
 * <p>Con {@code @GeneratedValue} sin más, Hibernate usa una secuencia por entidad,
 * {@code <tabla>_seq} de 50 en 50, la misma que crea {@code V1__esquema_inicial.sql}.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    public Long id;
}
