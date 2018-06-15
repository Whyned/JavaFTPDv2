#!/bin/bash

packname="DDRFTPD2-$(date +%F_%T)"
packagefolder="package"
shortcutpackage=$packagefolder/$packname
jarname="DDRFTPD.jar"
echo "Bedenk dass du den FTPD erst bauen musst, bevor du das hier ausführen kannst ;)"
mkdir -p $packagefolder
mkdir $shortcutpackage
cp target/DDRFTPServer-1.0-SNAPSHOT-jar-with-dependencies.jar $shortcutpackage/$jarname
cp -r default_files/* $shortcutpackage
echo "Done. Schau mal in $shortcutpackage"
