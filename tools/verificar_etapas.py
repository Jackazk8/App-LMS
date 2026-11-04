#!/usr/bin/env python3
"""Compatibilidad: usa el verificador vigente de todos los módulos y sus huellas."""
import runpy
from pathlib import Path
runpy.run_path(str(Path(__file__).with_name('verificar_modulos.py')),run_name='__main__')
