#!/bin/bash
#
# general compile script for Formula applications
#
export LANG=en_US.UTF-8

APP=solen

./mvnw -U clean install -DskipTests
if [ $? -ne 0 ]; then
  echo "compile error!"
  exit 2
fi

# packaging
rm -rf output
mkdir -p output/$APP/lib

cp target/$APP.jar output/$APP/lib
cp -r bin output/$APP/bin
tar zcf output/$APP.tar.gz -C output/$APP .
rm -rf output/$APP
