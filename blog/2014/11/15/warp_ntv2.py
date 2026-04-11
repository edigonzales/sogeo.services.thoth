#!/usr/bin/python
# -*- coding: utf-8 -*-

from osgeo import ogr, osr
import os

S_SRS = "+proj=somerc +lat_0=46.952405555555555N +lon_0=7.439583333333333E +ellps=bessel +x_0=600000 +y_0=200000 +towgs84=674.374,15.056,405.346 +units=m +units=m +k_0=1 +nadgrids=./chenyx06/chenyx06a.gsb"
T_SRS = "+proj=somerc +lat_0=46.952405555555555N +lon_0=7.439583333333333E +ellps=bessel +x_0=2600000 +y_0=1200000 +towgs84=674.374,15.056,405.346 +units=m +k_0=1 +nadgrids=@null"

ogr.UseExceptions() 

shp = ogr.Open("mosaic/ortho2014.shp")
layer = shp.GetLayer(0)

for feature in layer:
    infileName = feature.GetField('location')
    geom = feature.GetGeometryRef()
    env = geom.GetEnvelope()

    minX = int(env[0] + 0.001 + 2000000)
    minY = int(env[2] + 0.001 + 1000000)
    maxX = int(env[1] + 0.001 + 2000000)
    maxY = int(env[3] + 0.001 + 1000000)
    
    outfileName = str(minX)[0:4] + str(minY)[0:4] + "_12_5cm.tif"
    outfileName = os.path.join(os.path.dirname(infileName), "lv95", outfileName) 
        
    cmd = "/usr/local/gdal/gdal-dev/bin/gdalwarp -s_srs \"" + S_SRS + "\" -t_srs \"" + T_SRS + "\" -te "  + str(minX) + " " +  str(minY) + " " +  str(maxX) + " " +  str(maxY) 
    cmd += " -tr 0.125 0.125 -wo NUM_THREADS=ALL_CPUS -co 'PHOTOMETRIC=RGB' -co 'TILED=YES' -co 'PROFILE=GeoTIFF'"  
    cmd += " -co 'INTERLEAVE=PIXEL' -co 'COMPRESS=DEFLATE' -co 'BLOCKXSIZE=512' -co 'BLOCKYSIZE=512'" 
    vrt = "/opt/Geodaten/ch/so/kva/orthofoto/2014/rgb/12_5cm/ortho2014rgb.vrt"
    cmd += " -r bilinear " + vrt + " " + outfileName
    os.system(cmd)

    cmd = "/usr/local/gdal/gdal-dev/bin/gdal_edit.py -a_srs EPSG:2056 " + outfileName
    os.system(cmd)
    
    cmd = "/usr/local/gdal/gdal-dev/bin/gdaladdo -r nearest --config COMPRESS_OVERVIEW DEFLATE --config GDAL_TIFF_OVR_BLOCKSIZE 512 " + outfileName + " 2 4 8 16 32 64 128"
    os.system(cmd)
