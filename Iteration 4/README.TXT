/*
  FILENAME - README.txt
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DATE - 03/24/2018
  DETAILS - Each .java file has its own documentation.

  Source files
  Client.java        - Client program
  ErrorSim.java      - Intermediate Host/Error Simulator program
  Server.java        - Server program
  CommonMethods.java - Contains commonMethods used by Client, ErrorSim and Server 

  This README is for the Iteration 4 submission for the final project.
  Code runs according to the requirements. All files run separately.

  "./ClientFiles/"
  TFTP Client reads to and writes files from this directory
  
  "./ServerFiles/"
  TFTP Server reads from and writes files to this directory

  Set up Instructions
  1)Open new project in eclipse with code folder
  2)Open 3 consoles windows in eclipse.
  3)Run Server program
  4)Run ErrorSim program
  5)Run Client program
  6)Enter input for ErrorSim, example below

  ============================
  TFTP ErrorSim is running.

  Would you like to run the ErrorSim in (t)est or (n)ormal mode? t

  Test Mode Options: 
  (1)	- Lose a Packet
  (2)	- Delay a Packet
  (3)	- Duplicate a Packet
  (4)	- Send an Illegal Packet
  (5)	- Send from Unknown TID
  (c)	- Cancel Test Mode
  Please choose an option: 5

  Packet from an unknown TID!
  (1)	- Data
  (2)	- Acknowledgement
  (3)	- Error

  Which packet type is being sent from an unknown TID?  1
  What packet/block number would you like to send? 2
  DATA Packet# 2 will be sent from an unknown TID.
  ============================

  7)Enter input for Client, example below

  ============================
  TFTP Client is running.
  
  Commands:
  (s)tart   - Start a file transfer
  (o)ptions - Change parameters for this application
  (q)uit    - Ends the program after all file transfers are completed

  Please choose an option: s
  Enter file name: L
  Is it a (r)ead or (w)rite?: w
  ============================
  
  8)Enter q in Server Console when instructed to shutdown Server	  

*/
