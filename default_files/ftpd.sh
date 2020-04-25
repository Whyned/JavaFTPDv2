#!/bin/bash
java="java"
jar="DDRFTPD.jar"
parameter="-Xmx128m"
execa=""

printcmdline=0
background=1

# Handle Arguments
if [ "$1" == "--debug" ];
then
	printcmdline=1
	background=0
	execa=""
fi

# Build CMDLine

if [ -f ./java/bin/java ];
then
	java="./java/bin/java"
fi

cmdline=$java" "$parameter" -jar "$jar

if [[ $background -eq 1 ]];
then
	cmdline=$cmdline"  2>&1 &"
fi

if [ "$execa" != "" ];
then
	cmdline='(trap "" SIGINT; exec -a "'$execa'" '$cmdline
	cmdline=$cmdline")"
fi

if [[ $printcmdline -eq 1 ]];
then
	echo "CMDLINE: $cmdline"
	echo "--------------------"
fi

eval $cmdline
