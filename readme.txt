Impulse is a utility for testing network traffic interactions with clients and/or servers.
Impulse at its simplest is a server that listens on a port(s), accepts
connections, reads data, and then acts on that data. The acts that can be 
performed are implemented through transform plugins. 

Reasoning
The reasons that someone would want to use a tool like this are:
1. You could write a transform that simulates a server.
2. You could use it as a tunnel for testing:
   - disconnects.
   - tcp pushback by stopping to read from the client socket.
   - delaying of messages either on the way to or back from a server.
   - dropping of messages either on the way to or back from a server.
   - inserting duplicate messages.
   - after a disconnect you can control how long to delay accepting new connections.
The testing aspects could be done within the client code but it is complicated 
and it pollutes the code as the number of test types increase.

Transforms
Transforms take data read from the network, then does something with it, 
and then passes it on to the next transform. By creating simple transforms 
and having the ability to chain them together in different ways creates a 
simple language for manipulating data. Transforms are written in Java and the
description/properties/operations about the transforms can be viewed in the Javadoc.
A simple example would be an echo application where you would need 3 transforms:
1. a SocketServer to listen on a port.
2. a Server that handles read/writing to a client.
3. an echoer that takes what's read from the Server and it writes it back.

Configuration
The transform plugins to be used are described in a configuration file which
is passed as an argument to Impulse's main. The configuration file contains 
property values for the transforms as well as how they all chain together. 
Sample config files can be found in the config directory.
The basic syntax follows (order is not important):
- transform.[name].classname=[java classname] - for declaring a transform.
- transform.[name].[property1]=[someValue] - for setting property1 value to someValue on a transform.
What properties can be set are documented in the transform javadoc.
- link.out.[output transform].[output name]=[input transform].[input name]([input arguments,...]) - for creating connections.
One output can go to many inputs. In this case the outputs need in a semi-colon ordered list of invocation:
link.out.[output transform].[output name]=[input transform 1].[input name]([args]);[input transform 2].[input name]([args]).

Getting Started
For starting Impulse, there is a run.sh and run.bat startup files that use the
following syntax:
Impulse [-Dkey=value ...] configFile 

Alternately you can specify the system property 
-DconfigFile=<config file> for specifying a config file. The run.sh and run.bat
startup files configure JMX to use port 12333, and to use the Log4J.properties file 
in the current directory. For documentation on the transforms look in the docs directory
for the transform javadocs under the package "com.grb.impulse.plugins". Inputs and
Outputs are labelled to see what are valid chains that can be made. The best resource
is always the sample config files. Logging can be configured in three ways:
- configuring the log4j.properties file.
- setting the log4jLogLevel property on a transform in a transform properties file.
- setting the log4jLogLevel property on a transform in JMX.
- setting the log4jLogLevel property through the command line.
Transform properties in general can be configured through the command line. 
For example: -Dtransform.tunnel.serverIPPort=192.168.160.128:55555. 
Transform properties "serverIPPort" and "log4jLogLevel" can be wildcarded.
If you want to turn on debugging for all transforms you can specify 
 -Dtransform.*.log4jLogLevel=DEBUG on the command line or 
transform.*.log4jLogLevel=DEBUG in the config file.
Similarly, you can specify -Dtransform.*.serverIPPort=192.168.160.128:55555
on the command line or transform.*.serverIPPort=192.168.160.128:55555 in the config 
file to configure all tunnels. This is useful if you have a config file that listens
on many ports and each port creates a tunnel with different transforms. This allows 
the user to specify the ip address and port for all the tunnels in one line.

Transform Lifetime
Transforms can be auto started (statically created) or created on demand. An example 
of an auto started transform is a socket server. Typically you would want the socket 
server to be started and listening on a port at startup. This is accomplished by 
specifying autoStart=true for the socket server in the config file. An example of a 
created on demand transform is a tunnel where you don't want the tunnel created until 
a connection has been accepted by a socket server. You would connect the socket 
server's output onNewConnection to the tunnel's onNewConnection input. Since the 
tunnel was not auto started, the tunnel is created as well as all the transforms in 
its chain. This configuration will create a new tunnel for every new connection. 
In order to distinguish one tunnel from another in JMX, the transform name will be 
the same name as assigned in the config file  with a index number concatenated on the end.
There will also be an entry with the exact same name as in the config file but that 
is used as a template for the numbered entries. Modifying the template will have an 
affect on new transforms based on that template.
 
Running in Eclipse
In order to run in eclipse and debug its handy to have the following VM arguments specified.
-Dlog4j.debug -Dlog4j.configuration="file:log4j.properties" -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=12333 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
