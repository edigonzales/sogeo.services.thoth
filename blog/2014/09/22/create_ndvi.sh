#!/bin/bash

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/gdal/gdal-dev/lib
export PYTHONPATH=$PYTHONPATH:/usr/local/lib/python2.6/dist-packages/GDAL-2.0.0-py2.6-linux-x86_64.egg

for FILE in /opt/Geodaten/ch/so/kva/orthofoto/2014/cir/12_5cm/*.tif
do
  BASENAME=$(basename $FILE .tif)
  OUTFILE=/home/stefan/Geodaten/NDVI_2014/${BASENAME}.tif
  OUTFILE_PREP=/home/stefan/Geodaten/NDVI_2014/${BASENAME}_prep.tif
  OUTFILE_TMP=/home/stefan/Geodaten/NDVI_2014/${BASENAME}_tmp.tif

  echo "Processing: ${BASENAME}.tif"

  cp $FILE $OUTFILE
  listgeo -tfw $OUTFILE
  rm $OUTFILE

  convert $FILE $OUTFILE_PREP

  convert -monitor $OUTFILE_PREP -fx '(u.r - u.g) / (u.r + u.g + 0.001)' $OUTFILE_TMP

  /usr/local/gdal/gdal-dev/bin/gdal_edit.py -a_srs EPSG:21781 $OUTFILE_TMP
  /usr/local/gdal/gdal-dev/bin/gdal_translate -co TILED=YES -co COMPRESS=LZW $OUTFILE_TMP $OUTFILE
  /usr/local/gdal/gdal-dev/bin/gdaladdo -r cubic --config COMPRESS_OVERVIEW LZW $OUTFILE 2 4 8 16 32 64 128

  rm $OUTFILE_PREP
  rm $OUTFILE_TMP
done
