MissionControl
==============

MissionControl is a java application for (kind of) home automation.

Made for Raspberry pi, but should not be difficult to port to anything else supporting Java. 

The repo contains a NetBeans project (can be built using bare ant). Does not use any side libraries, instead relying on native applications for hardware interaction.

Architecture
------------

The core of the application is an event processing pipeline. Classes that receive user-input events (IR receivers), sensor input (temperature, IR beam interrupters), and system events (time) pump data into the pipe, whiel event consumers (light controllers, speech generators) take events from the pipe and do their job. Every set of devices related to one physical room should have it's own pipeline.

Physical implementation of external event sources consits of 2 engines.

1. A named pipe is listened to for commands by the application. This method of communication is used by the following back-ends:
 * IR remote receiver. For speed purposes, IR receiving code is implemented as a standalone C-program that sends data to this pipe (For implementation, see https://github.com/positron96/rpi-ircontrol).
 * TODO: Internet website backend (Or it could be implemented as an in-process web-server)
2. Raspberry's UART is accessed via ser2net and Java sockets. Should be used by devices talking to Raspberry via UART. A simple protocol with a very basic collision detection is implemented, so many devices should work on one UART. Devices supported fo far:
 * Arduino-based people counter (IR led on one side of door, two IR-phototransistors on the other, connected by signal processing software)
