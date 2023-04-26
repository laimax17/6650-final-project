# 6650-final-project

Our project is a multi-person chat room designed to allow users to communicate with each other by sending and receiving messages in real-time.

A distributed system is used to store the chat history. The chat history is replicated across multiple replicas for high availability and redundancy. To reach consensus across the replicas, we implemented Paxos in the distributed system. Additionally, fault-tolerance techniques are in place to withstand possible server failures and to ensure data consistency.

# How to run
* Clone the repository
* Go to src directory
* Deploy server: run script`./deploy.sh <port>`, `port` is the port number on which the server will listen to
* Start clients: run script `./run_client.sh <port> <client-name>`, `port` is the port number to the server, `client-name` is the name of the client and should be unique. You can run multiple clients on different terminals to simulate multiple users chatting with each other.