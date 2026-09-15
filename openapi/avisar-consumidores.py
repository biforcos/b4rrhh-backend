#!/usr/bin/env python3
"""Avisa de que el contrato ha cambiado, nombrando a los consumidores que se quedan atras.

`b4rrhh/workspace#5`. El contrato vive aqui y lo consumen DOS repositorios —el frontend y el
designer—, que versionan su copia y la comprueban contra `main` en cada build (`workspace#4`). Esa
puesta al dia es manual y ocurre en otro sitio, asi que depende de que alguien se acuerde. Van tres
veces que no.

**Lo que falla no es la memoria de nadie: es que el recordatorio vivia en el sitio equivocado.**
Vivia en el texto de cada issue del backend, escrito por alguien que esta pensando en el backend, y
ningun issue tiene por que acordarse del designer. El que tiene el dato es este repositorio: es el
unico que sabe que el contrato ha cambiado, y sabe quien lo consume.

Asi que el aviso sale de aqui, y no se limita a recordar: **mira**. Se trae la copia de cada
consumidor y dice cual esta al dia y cual no, por su nombre y con la orden exacta que lo arregla.

## Por que avisa y no se pone rojo

Porque el rojo caeria en el sitio equivocado por segunda vez, sólo que al reves.

Cuando este repositorio cambia el contrato, sus consumidores **tienen que** quedarse atras un rato:
su candado se trae el contrato de `backend@main`, asi que no pueden ponerse al dia hasta que el
commit esta en `main` —es decir, hasta despues de que este aviso corra—. Fallar aqui seria fallar
por una ventana que el propio orden de las cosas obliga a abrir, y en un pipeline que ademas
despliega. Un rojo que no se puede apagar en el momento se aprende a ignorar, y un guardarrail que
se ignora deja de proteger justo en el caso que importa (`backend#14`).

Si algun dia se decide lo contrario, la palanca es `--fallar` y es una palabra en el workflow.

## De donde saca la copia de cada consumidor, por este orden

1. `GITEA_URL` + `CONTRACT_TOKEN` — la API de Gitea. Es lo que usa el CI, que no tiene los
   repositorios hermanos al lado. El token es el mismo secreto de organizacion que ya usan los dos
   candados, y le basta con LECTURA.

   `GITEA_URL` es la BASE (`http://host:puerto`) y no la URL del fichero, que es lo que vale
   `CONTRACT_URL` en los dos consumidores. Son nombres distintos a proposito: aqui hay que pedir
   dos ficheros y la URL se construye, y reutilizar el nombre de alla con otra forma seria una
   trampa para el siguiente que lo lea.
2. El checkout hermano bajo `WORKSPACE_ROOT` (por defecto, el directorio que contiene este
   repositorio). Es lo que hay en la maquina de quien desarrolla, y asi no se le pide un token.

Ojo con Gitea y los permisos: los cinco repositorios son privados y **contesta 404, no 401**, a
quien no tiene permiso. Es lo correcto y engaña al depurar.
"""

from __future__ import annotations

import argparse
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

CONTRATO = "personnel-administration-api.yaml"

#: Los dos que consumen el contrato. El nombre del repositorio en Gitea, la carpeta del checkout
#: hermano, y la orden que pone al dia esa copia.
CONSUMIDORES = (
    ("frontend", "b4rrhh_frontend", "npm run api:refresh"),
    ("designer", "b4rrhh_designer", "npm run api:refresh"),
)

RAIZ = Path(__file__).resolve().parent.parent


def normalizar(texto: str) -> str:
    """Los finales de linea no son el contrato. Windows no tiene por que sacar un rojo aqui."""
    return texto.replace("\r\n", "\n")


def solo_en(referencia: list[str], local: list[str]) -> list[str]:
    """Las lineas de `referencia` que no estan en `local`, en el orden del fichero.

    No es un diff de verdad —no alinea bloques— pero contesta la pregunta de quien lo lee: que trae
    lo nuevo que alli no esta. Es el mismo criterio que usan los candados de los dos consumidores,
    a proposito: dos maneras de contar la misma distancia se contradicen el dia que importa.
    """
    disponibles: dict[str, int] = {}
    for linea in local:
        disponibles[linea] = disponibles.get(linea, 0) + 1

    sueltas = []
    for linea in referencia:
        quedan = disponibles.get(linea, 0)
        if quedan > 0:
            disponibles[linea] = quedan - 1
        else:
            sueltas.append(linea)
    return sueltas


def leer_de_gitea(repo: str, base_url: str, token: str) -> str | None:
    url = f"{base_url.rstrip('/')}/api/v1/repos/b4rrhh/{repo}/raw/openapi/{CONTRATO}?ref=main"
    peticion = urllib.request.Request(url, headers={"Authorization": f"token {token}"})
    try:
        with urllib.request.urlopen(peticion, timeout=30) as respuesta:
            return respuesta.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        # 404 es tambien «no tienes permiso»: los repositorios son privados.
        print(f"  no se ha podido leer {repo}: HTTP {error.code}", file=sys.stderr)
        return None
    except OSError as error:
        print(f"  no se ha podido leer {repo}: {error}", file=sys.stderr)
        return None


def leer_del_hermano(carpeta: str, workspace_root: Path) -> str | None:
    ruta = workspace_root / carpeta / "openapi" / CONTRATO
    if not ruta.is_file():
        print(f"  no hay checkout hermano en {ruta}", file=sys.stderr)
        return None
    return ruta.read_text(encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--fallar",
        action="store_true",
        help="salir con codigo 1 si algun consumidor esta por detras (por defecto solo avisa)",
    )
    args = parser.parse_args()

    referencia = normalizar((RAIZ / "openapi" / CONTRATO).read_text(encoding="utf-8"))

    base_url = os.environ.get("GITEA_URL", "").strip()
    token = os.environ.get("CONTRACT_TOKEN", "").strip()
    workspace_root = Path(os.environ.get("WORKSPACE_ROOT", "").strip() or RAIZ.parent)
    por_gitea = bool(base_url and token)

    origen = f"Gitea ({base_url})" if por_gitea else f"checkouts hermanos ({workspace_root})"
    print(f"El contrato de este commit, comparado con el de cada consumidor — origen: {origen}\n")

    atrasados: list[tuple[str, str, int]] = []
    desconocidos: list[str] = []

    for repo, carpeta, orden in CONSUMIDORES:
        copia = (
            leer_de_gitea(repo, base_url, token)
            if por_gitea
            else leer_del_hermano(carpeta, workspace_root)
        )
        if copia is None:
            desconocidos.append(repo)
            print(f"  b4rrhh/{repo}: NO SE SABE — no se ha podido leer su copia\n")
            continue

        faltan = solo_en(referencia.split("\n"), normalizar(copia).split("\n"))
        if faltan:
            atrasados.append((repo, orden, len(faltan)))
            print(f"  b4rrhh/{repo}: POR DETRAS — le faltan {len(faltan)} linea(s)")
            for linea in faltan[:6]:
                print(f"      + {linea}")
            if len(faltan) > 6:
                print(f"      + ... y {len(faltan) - 6} mas")
            print(f"      se arregla con:  cd {carpeta} && {orden}\n")
        else:
            print(f"  b4rrhh/{repo}: al dia\n")

    if atrasados:
        print("Este commit toca el contrato y deja atras a " + ", ".join(r for r, _, _ in atrasados) + ".")
        print("Su CI se pondra rojo en cuanto corra, y el rojo le saldra a quien no lo rompio.")
    elif not desconocidos:
        print("Los dos consumidores estan al dia. No hay nada que hacer.")

    escribir_resumen(atrasados, desconocidos)

    if args.fallar and atrasados:
        return 1
    return 0


def escribir_resumen(
    atrasados: list[tuple[str, str, int]], desconocidos: list[str]
) -> None:
    """Deja el aviso en el resumen del job, que es la pagina que se mira, no el log que se entierra."""
    destino = os.environ.get("GITHUB_STEP_SUMMARY", "").strip()
    if not destino:
        return

    lineas = ["## Consumidores del contrato", ""]
    if atrasados:
        lineas.append("Este commit toca `openapi/` y estos repositorios se quedan atras:")
        lineas.append("")
        lineas.append("| repositorio | lineas que le faltan | se arregla con |")
        lineas.append("|---|---:|---|")
        for repo, orden, cuantas in atrasados:
            lineas.append(f"| `b4rrhh/{repo}` | {cuantas} | `{orden}` |")
    else:
        lineas.append("Este commit toca `openapi/` y **los dos consumidores estan al dia**.")
    if desconocidos:
        lineas.append("")
        lineas.append("No se ha podido leer la copia de: " + ", ".join(f"`{r}`" for r in desconocidos) + ".")

    with open(destino, "a", encoding="utf-8") as resumen:
        resumen.write("\n".join(lineas) + "\n")


if __name__ == "__main__":
    sys.exit(main())
