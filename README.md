# Node project

This program creates nodes that communicate with each other using TCP. Each node runs as its own thread. A network node will connect to another node if a gateway is provided. When a network node receives a request, it will create another thread to process the commands.

If a request comes from another node asking for a connection, the handshake procedure will be initiated, and the network nodes will exchange their IDs and add them to the list. If a client requests resources and the node does not have the requested type available, the node will communicate with other nodes to locate the missing resource.

Once the main node confirms that all required resources are available, it will send the command ACTIVATE_ALLOCATOR to reserve those resources on the nodes where they are free. If none of the nodes have the requested resources available, the network node will send a FAILED message to the client.

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
