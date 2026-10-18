-- =========================================================
-- La foto de un empleado se guarda por su clave y la sirve la API (backend#177)
-- =========================================================
--
-- employee.photo_url guardaba la URL entera del almacen tal como la veia el
-- backend que recibio la foto: http://localhost:9000/b4rrhh-employee-photos/...
-- en local, http://minio:9000/... en la demo. Ninguna de las dos le sirve a un
-- navegador fuera de esa maquina, asi que la demo no podia ensenar una foto
-- aunque la semilla la llevara.
--
-- Desde aqui guarda solo la clave del objeto:
--     photos/ESP/INTERNAL/EMP000025/<uuid>.jpg
-- y el enlace lo compone la API al leer (/api/employees/.../photo?v=<uuid>).
-- La columna no se renombra: el nombre es historia, el contenido es lo que cambia.
--
-- Todo lo que hay antes de /b4rrhh-employee-photos/ sobra, venga del host que
-- venga. Y despues no puede quedar nada que no tenga forma de clave: una fila
-- asi seria un enlace roto en la ficha, y es mejor que la migracion pare y lo
-- diga a que lo descubra un visitante.

update employee.employee
   set photo_url = substring(photo_url from '/b4rrhh-employee-photos/(.+)$')
 where photo_url like '%/b4rrhh-employee-photos/%';

do $$
declare
    malas bigint;
    ejemplo text;
begin
    select count(*), min(photo_url)
      into malas, ejemplo
      from employee.employee
     where photo_url is not null
       and photo_url !~ '^photos/[^/]+/[^/]+/[^/]+/[0-9a-f-]{36}\.jpg$';
    if malas > 0 then
        raise exception 'V182: % fotos de empleado no tienen forma de clave tras la conversion (por ejemplo: %)',
            malas, ejemplo;
    end if;
end $$;
