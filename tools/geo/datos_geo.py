"""Los ficheros de datos del maestro geo (backend#154, ADR-078) y su procedimiento anual.

Dos usos, los dos desde la raiz de b4rrhh_backend:

    python tools/geo/datos_geo.py inicial
        Baja las relaciones de municipios del INE a 1 de enero de 2021 a 2025 y los codigos
        postales de GeoNames, y escribe los dos ficheros que carga la V176:
            src/main/resources/db/geo/municipios-ine-2021-2025.tsv
            src/main/resources/db/geo/codigos-postales-geonames.tsv
        Esos dos ficheros quedan congelados en cuanto la V176 se aplica: su checksum es el de
        la migracion. No se regeneran nunca encima.

    python tools/geo/datos_geo.py anual 2026
        Compara la relacion del INE a 1 de enero de 2026 con la de 2025 y escribe en la salida
        el SQL de la migracion siguiente: altas (fila nueva desde el 1 de enero), bajas
        (end_date el 31 de diciembre anterior) y renombres (el nombre nuevo, mismo codigo).
        Ver LEEME.md junto a los ficheros de datos.

Solo usa la biblioteca estandar.
"""
import collections
import csv
import datetime
import io
import re
import sys
import unicodedata
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

INE = 'https://www.ine.es/daco/daco42/codmun/diccionario{yy:02d}.xlsx'
GEONAMES = 'https://download.geonames.org/export/zip/ES.zip'
DATOS = Path('src/main/resources/db/geo')
PRIMERA, ULTIMA = 2021, 2025


def bajar(url):
    with urllib.request.urlopen(url, timeout=120) as r:
        return r.read()


def filas_xlsx(contenido):
    """Las filas de la primera hoja de un .xlsx, como dict columna -> valor."""
    ns = {'m': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
    z = zipfile.ZipFile(io.BytesIO(contenido))
    compartidas = []
    if 'xl/sharedStrings.xml' in z.namelist():
        for si in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('m:si', ns):
            compartidas.append(''.join(t.text or '' for t in si.iter('{%s}t' % ns['m'])))
    hoja = ET.fromstring(z.read('xl/worksheets/sheet1.xml'))
    for fila in hoja.iter('{%s}row' % ns['m']):
        salida = {}
        for c in fila.findall('m:c', ns):
            columna = re.match(r'[A-Z]+', c.get('r')).group(0)
            v, tipo = c.find('m:v', ns), c.get('t')
            if tipo == 'inlineStr':
                salida[columna] = ''.join(x.text or '' for x in c.iter('{%s}t' % ns['m']))
            elif v is None:
                salida[columna] = None
            elif tipo == 's':
                salida[columna] = compartidas[int(v.text)]
            else:
                salida[columna] = v.text
        yield salida


def relacion(anyo):
    """Codigo INE de 5 cifras -> nombre, de la relacion a 1 de enero de `anyo`."""
    municipios = {}
    for f in filas_xlsx(bajar(INE.format(yy=anyo % 100))):
        cpro, cmun = f.get('B'), f.get('C')
        if cpro and cpro.isdigit() and cmun and cmun.isdigit():
            municipios[cpro + cmun] = f['E'].strip()
    if not municipios:
        sys.exit(f'la relacion de {anyo} ha llegado vacia')
    return municipios


def normal(texto):
    """Minusculas sin tildes, el articulo pospuesto delante, y una forma por idioma."""
    texto = unicodedata.normalize('NFD', texto.lower())
    texto = ''.join(c for c in texto if unicodedata.category(c) != 'Mn').replace("'", ' ').replace('-', ' ')
    formas = set()
    for parte in texto.split('/'):
        parte = parte.strip()
        m = re.match(r'(.*), *(la|el|las|los|les|l|o|a|os|as|es|sa|s)$', parte)
        if m:
            parte = m.group(2) + ' ' + m.group(1)
        formas.add(re.sub(r'\s+', ' ', parte).strip())
    return formas


def inicial():
    relaciones = {anyo: relacion(anyo) for anyo in range(PRIMERA, ULTIMA + 1)}
    ultima = relaciones[ULTIMA]
    desde = {}
    for anyo in range(PRIMERA, ULTIMA + 1):
        for codigo in relaciones[anyo]:
            desde.setdefault(codigo, anyo)
    bajas = set(desde) - set(ultima)
    if bajas:
        sys.exit(f'hay bajas entre {PRIMERA} y {ULTIMA} y este guion no las escribe: {sorted(bajas)}')

    DATOS.mkdir(parents=True, exist_ok=True)
    with open(DATOS / 'municipios-ine-2021-2025.tsv', 'w', encoding='utf-8', newline='\n') as f:
        f.write('codigo\tnombre\tprovincia\tdesde\n')
        for codigo in sorted(ultima):
            f.write(f'{codigo}\t{ultima[codigo]}\t{codigo[:2]}\t{desde[codigo]}-01-01\n')

    # El municipio sugerido sale del nombre del lugar, no de la columna de municipio de
    # GeoNames: esa columna se equivoca en el 11 % de los codigos postales que se pueden
    # comprobar (el 46250 es L'Alcudia y dice Sagunto). Un lugar cuyo nombre es el de un
    # municipio de la misma provincia sugiere ese municipio; si un codigo postal sugiere dos,
    # no sugiere ninguno.
    indice = collections.defaultdict(set)
    for codigo, nombre in ultima.items():
        for forma in normal(nombre):
            indice[(codigo[:2], forma)].add(codigo)
    sugeridos = collections.defaultdict(set)
    codigos_postales = set()
    with zipfile.ZipFile(io.BytesIO(bajar(GEONAMES))) as z:
        for fila in csv.reader(io.TextIOWrapper(z.open('ES.txt'), encoding='utf-8'), delimiter='\t'):
            cp, lugar = fila[1], fila[2]
            if not re.fullmatch(r'\d{5}', cp):
                continue
            codigos_postales.add(cp)
            for forma in normal(lugar):
                sugeridos[cp] |= indice.get((cp[:2], forma), set())
    with open(DATOS / 'codigos-postales-geonames.tsv', 'w', encoding='utf-8', newline='\n') as f:
        f.write('codigo_postal\tmunicipio_sugerido\n')
        for cp in sorted(codigos_postales):
            unico = sugeridos[cp]
            f.write(f'{cp}\t{next(iter(unico)) if len(unico) == 1 else ""}\n')
    con = sum(1 for cp in codigos_postales if len(sugeridos[cp]) == 1)
    print(f'{len(ultima)} municipios; {len(codigos_postales)} codigos postales, {con} con municipio sugerido')


def anual(anyo):
    anterior, nueva = relacion(anyo - 1), relacion(anyo)
    desde = datetime.date(anyo, 1, 1)
    hasta = desde - datetime.timedelta(days=1)
    q = lambda s: "'" + s.replace("'", "''") + "'"
    print(f'-- Relacion de municipios del INE a 1 de enero de {anyo} contra la de {anyo - 1}')
    print(f'-- (datos_geo.py anual {anyo}, {datetime.date.today()}).')
    for codigo in sorted(set(nueva) - set(anterior)):
        print(f"insert into geo.municipality (country_code, code, name, province_code, start_date) "
              f"values ('ESP', '{codigo}', {q(nueva[codigo])}, '{codigo[:2]}', date '{desde}');")
    for codigo in sorted(set(anterior) - set(nueva)):
        print(f"update geo.municipality set end_date = date '{hasta}' "
              f"where country_code = 'ESP' and code = '{codigo}';")
    for codigo in sorted(set(anterior) & set(nueva)):
        if anterior[codigo] != nueva[codigo]:
            print(f"update geo.municipality set name = {q(nueva[codigo])} "
                  f"where country_code = 'ESP' and code = '{codigo}';  -- era {anterior[codigo]}")


if __name__ == '__main__':
    if sys.argv[1:] == ['inicial']:
        inicial()
    elif len(sys.argv) == 3 and sys.argv[1] == 'anual':
        anual(int(sys.argv[2]))
    else:
        sys.exit(__doc__)
