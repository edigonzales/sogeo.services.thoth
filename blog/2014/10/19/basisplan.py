# -*- coding: utf-8 -*-
from PyQt4.QtCore import *
from PyQt4.QtGui import *
from qgis.core import *
from qgis.gui import *

import os
import sys

# Aktuelles Verzeichnis
current_dir = os.path.dirname(os.path.realpath(__file__))

# QGIS initialisieren
app = QApplication(sys.argv)        
QgsApplication.setPrefixPath("/home/stefan/Apps/qgis_master", True)
QgsApplication.initQgis()

# QGIS-Projekt laden
QgsProject.instance().setFileName(os.path.join(current_dir,  "bpav5000sw.qgs"))
if not QgsProject.instance().read():
    sys.exit("QGIS-Projekt nicht gefunden.")

# List mit sämtlichen Layer im QGIS-Projekt
lst = []
layerTreeRoot = QgsProject.instance().layerTreeRoot()
for id in layerTreeRoot.findLayerIds():
    node = layerTreeRoot.findLayer(id)
    lst.append(id)
    
# Layer mit Blatteinteilung laden
layer_name =  "blatteinteilung"
vlayer = QgsVectorLayer(os.path.join(current_dir, "basisplan.gpkg") + "|layername=blatteinteilung", "Blatteinteilung", "ogr")
if not vlayer.isValid():
    sys.exit("Blatteinteilung konnte nicht geladen werden.")
    
# Rasterkarten erstellen
iter = vlayer.getFeatures()
for feature in iter:
    idx = vlayer.fieldNameIndex('nummer')
    nummer = feature.attributes()[idx].toString() 

    # Ausschnitt und Grösse der Karte berechnen
    dpi = 508
    scale = 5000
    
    geom = feature.geometry()
    p1 = geom.vertexAt(0)
    p2 = geom.vertexAt(2)
    
    rect = QgsRectangle(p1, p2) 
    
    dx = rect.width()
    dy = rect.height()

    width = (dx/scale) / 0.0254 * dpi
    height = (dy/scale) / 0.0254 * dpi

    # Einstellungen für Kartenrenderer
    mapSettings = QgsMapSettings()
    mapSettings.setMapUnits(0)
    mapSettings.setExtent(rect)
    mapSettings.setOutputDpi(dpi)
    mapSettings.setOutputSize(QSize(width, height))
    mapSettings.setLayers(lst)
    mapSettings.setFlags(QgsMapSettings.DrawLabeling)
    
    # Karte zeichnen
    img = QImage(QSize(width, height), QImage.Format_Mono)
    img.setDotsPerMeterX(dpi / 25.4 * 1000)
    img.setDotsPerMeterY(dpi / 25.4 * 1000)
        
    p = QPainter()
    p.begin(img)
    
    mapRenderer = QgsMapRendererCustomPainterJob(mapSettings, p)
    mapRenderer.start()
    mapRenderer.waitForFinished()

    p.end()

    img.save(os.path.join("/tmp", "bpav" + str(scale) + "_" + str(nummer) + str(".png")), "png")

# Layer mit Blatteinteilung löschen
del vlayer

# QGIS schliessen
QgsApplication.exitQgis()
