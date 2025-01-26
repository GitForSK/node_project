# Node project

This program create nodes which communicate with each other using TCP. Each node is its own thred. Network node will connect to another node if gateway was passed. When Network node will receive the request it will create another thread to fulfill the commands. If request coming from the another node asking for connection, the handshake procedure will be started and then the network nodes will exchange theirs ids and add them to the list. If client request the resources and node do not have this type available, then asked node will communicate to the other nodes for the resource it did not have. The moment the main node have the information that the all resources are available it will send command ACTIVATE_ALLOCATOR to reserve those resources on the nodes where they were free. If any of the node have given resources available then network node will send message FAILED to the client.

## Compling:
javac NetworkNode.java
javac NetworkClient.java

## Running commands:
```
start java NetworkNode -ident <id> -tcpport <TCP port number> -gateway <address>:<port> <resource (template X:N, where X is a capital letter and N is number)>
```
```
java NetworkClient -ident <id> -gateway <address>:<port> <resource (template X:N, where X is a capital letter and N is number)>
```
