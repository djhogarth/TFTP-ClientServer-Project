/*
  FILENAME - README.txt
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DATE - 02/03/2018
  DETAILS - Each .java file has its own documentation.

  Source files in code/
  Client.java - Client program
  ErrorSim.java - Intermediate Host/Error Simulator program
  Server.java - Server program

  This README is for the Iteration 1 submission for the final project.
  Code runs according to the requirements. All files run separately.
  Input sanitization hasn't been implemented, so please be careful when entering file names/paths.

  We added the Working Directory output to the Client.java file to aid us/you when finding files.
  Place your files in the directory to make things easier, for now.

  Test case:
  TFTP Client is running.

  Set up Instructions
  1)Open new project in eclipse with code folder
  2)Open 3 consoles windows in eclipse.
  3)Run Server program
  4)Run ErrorSim program
  5)Run Client program
  6)User input example below

  Working Directory = /
  Enter file name: Large.txt
  Enter full file path: ./Large.txt
  Is it a (r)ead or (w)rite?: w
  If this is the last file, enter 'q'. <ENTER>    <--Enter q here to shutdown client
  
  TESTING
  1) To test the "File not Found" error, simply enter a file name not found in ClientFiles(WRQ) and ServerFiles(RRQ). 

  2)Access Violation:
	In order to test this case I have created a text file called ERROR2 located in 
	the clientFiles/ServerFiles, left click them and select properties, go
	to the security tab and then to advanced, from there you should be able
	to disable inheritance, apply and then try to read ERROR2, and we will
	be prompted by the error.

  7)Enter q in Server Console when instructed to shutdown Server
*/