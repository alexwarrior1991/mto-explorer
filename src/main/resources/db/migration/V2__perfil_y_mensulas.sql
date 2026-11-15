-- Una ménsula deja de apuntar a su perfil por (ep, track, profile_order) y pasa a tener una clave
-- foránea a profile(id): la relación @ManyToOne Cantilever.profile / @OneToMany Profile.cantilevers.

-- Los datos son una copia del Excel: se vacía y se vuelve a importar.
DELETE FROM cantilever;

ALTER TABLE cantilever
    DROP COLUMN ep,
    DROP COLUMN track,
    DROP COLUMN profile_id,
    DROP COLUMN profile_order;
DROP INDEX IF EXISTS cantilever_profile_idx;

ALTER TABLE cantilever
    ADD COLUMN profile_id BIGINT NOT NULL REFERENCES profile (id);
-- Una ménsula por posición (slot) en cada perfil; el índice sirve también para cargar las de un perfil.
CREATE UNIQUE INDEX cantilever_profile_slot_uq ON cantilever (profile_id, slot);

-- Lo que identifica un perfil, ahora garantizado por la base de datos.
CREATE UNIQUE INDEX profile_track_order_uq ON profile (ep, track, sort_order);
DROP INDEX IF EXISTS profile_track_order_idx;

-- Sin esto, al arrancar el maestro "no ha cambiado" y la tabla de ménsulas se quedaría vacía.
DELETE FROM master_load WHERE file_name = 'profile-master.xlsx';
