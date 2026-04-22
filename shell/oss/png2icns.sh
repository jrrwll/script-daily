#!/bin/bash
if [ $# -eq 0 ]; then
  echo "Usage: $0 input.png"
  exit 1
fi

SRC="$1"
NAME="${SRC%.*}"
mkdir "${NAME}.iconset"
cd "${NAME}.iconset"

# brew install imagemagick
magick "../$SRC" -resize 16x16      icon_16x16.png
magick "../$SRC" -resize 32x32      icon_16x16@2x.png
magick "../$SRC" -resize 32x32      icon_32x32.png
magick "../$SRC" -resize 64x64      icon_32x32@2x.png
magick "../$SRC" -resize 128x128    icon_128x128.png
magick "../$SRC" -resize 256x256    icon_128x128@2x.png
magick "../$SRC" -resize 256x256    icon_256x256.png
magick "../$SRC" -resize 512x512    icon_256x256@2x.png
magick "../$SRC" -resize 512x512    icon_512x512.png
magick "../$SRC" -resize 1024x1024  icon_512x512@2x.png

cd ..
iconutil -c icns "${NAME}.iconset"
echo "Generated ${NAME}.icns"
