# SYSC3303 : Real-Time Concurrent Systems

Final Project 

This is a multi-threaded TFTP Client-Server application built using Java that can send messages over the internet to other users or clients. 

Messages are encoded into bytes, wrapped in UDP data packets and then sent using datagram sockets. Messages within received packets are 
extracted and decoded from bytes into the original message and an acknowledge (ACK) packet is forwareded to the sender. Messages can be 
received in real-time from any computer running the java application. 

Each packet contins the OP code, and other information depending on the type of packet being sent. The types of packets that can be sent include 
read request packets, write request packets, error packets, ack packets, and data packets. 

This project uses programming and networking concepts such as: synchronization of java threads, multi-threading, client-server architecture, 
concurrency, UDP packets, sockets.
