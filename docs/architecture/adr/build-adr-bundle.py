#!/usr/bin/env python3
"""Genera ADR_BUNDLE.md a partir de los ADR del directorio (backend#17).

Un solo generador para Windows y Linux. Antes habia dos —un .ps1 y un .sh— y se
separaron en cuanto nadie los miro a la vez: el .ps1 leia los ADR con la pagina
de codigos de la maquina y llenaba el bundle de mojibake, los dos escribian
cabeceras distintas y el .sh no habia funcionado nunca. Aqui:

- se lee y se escribe UTF-8 explicito, sin depender de la consola;
- los finales de linea del bundle son siempre LF, los genere quien los genere;
- los ADR van en orden NUMERICO, por el numero del nombre, no por la cadena
  (ADR-28 cae entre el 27 y el 29 aunque le falte el cero);
- el destino se escribe por truncado, no por mv: en repos montados no siempre se
  puede borrar el fichero de destino.

Uso: python build-adr-bundle.py [directorio]
El directorio por defecto es el del propio script.
"""

import re
import sys
from datetime import datetime
from pathlib import Path

OUT_NAME = "ADR_BUNDLE.md"
NUMBER = re.compile(r"^ADR-(\d+)", re.IGNORECASE)


def anchor(name: str) -> str:
    return re.sub(r"[^a-z0-9]", "-", name.lower())


def sort_key(path: Path):
    match = NUMBER.match(path.name)
    number = int(match.group(1)) if match else float("inf")
    return (number, path.name)


def main() -> int:
    directory = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent
    out = directory / OUT_NAME
    files = sorted(
        (p for p in directory.glob("*.md") if p.is_file() and p.name != OUT_NAME),
        key=sort_key,
    )

    generated_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    parts = [
        "# ADR Bundle\n",
        "\n",
        "> Fichero generado automáticamente. No editar a mano.\n",
        f"> Fecha de generación: {generated_at}\n",
        "\n",
        "---\n",
        "\n",
        "## Índice\n",
        "\n",
    ]
    for path in files:
        parts.append(f"- [{path.name}](#file-{anchor(path.name)})\n")
    parts.append("\n---\n\n")

    for path in files:
        # newline=None al leer: un ADR con CRLF entra como LF y el bundle no
        # cambia de finales de linea segun quien lo genere.
        with open(path, encoding="utf-8", newline=None) as source:
            content = source.read()
        parts.append(
            "\n---\n\n"
            f"# FILE: {path.name}\n"
            f'<a name="file-{anchor(path.name)}"></a>\n'
            "\n"
            f"<!-- BEGIN FILE: {path.name} -->\n"
            "\n"
        )
        parts.append(content)
        parts.append(f"\n<!-- END FILE: {path.name} -->\n\n")

    with open(out, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("".join(parts))

    print(f"Generado: {out.name} ({len(files)} ficheros)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
