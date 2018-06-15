DDRFTPDv2
---------

How to build:
-------------

You first need:
- Maven2
- OpenJDK7 (newer should work too)
- bash

1. Run `./build_maven.sh` to build the ftpd.
2. Run `./create_package.sh` to build the package


How to run:
-----------

Simply executing the `ftpd.sh` should start the server. Make sure that you
have java installed. If you want to use a specific java version, you can
download a static compiled java, put it in the same folder as the ftpd.sh is.
The ftpd.sh script is looking for a `./java/bin/java` binary, if it finds one,
it uses it.

If you don't want that the ftpd suspends to the background and silences all
output, start it with `./ftpd.sh --debug`.

IRC Bot:
--------

To let the ftpd connect to an irc server you need to modify `/src/main/ddrftpserver/IRCBot.java` and set some class attributes to whatever you need. Besides that you also need to modify the config.ini file and enable the "[IRC]" section. 

Permissions:
------------

- free
- reload
- exec
- rescan



