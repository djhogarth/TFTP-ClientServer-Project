/*
  FILENAME - README.txt
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DATE - 02/17/2018
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

  "./ClientFiles/"
  TFTP Client reads to and writes files from this directory
  
  "./ServerFiles/"
  TFTP Server reads from and writes to this directory

  Set up Instructions
  1)Open new project in eclipse with code folder
  2)Open 3 consoles windows in eclipse.
  3)Run Server program
  4)Run ErrorSim program
  5)Run Client program
  6)User input example below

  ============================
  Test case:
  TFTP Client is running.
  
  Commands:
  (s)tart   - Start a file transfer
  (o)ptions - Change parameters for this application
  (q)uit    - Ends the program after all file transfers are completed

  Please choose an option: s
  Enter file name: L
  Is it a (r)ead or (w)rite?: w
  ============================

  7)Enter q in Server Console when instructed to shutdown Server
  
  Error code tests:
  
  1 --File not Found Error--
  In order to test the File not Found error, enter a file name that does not exist in the ClientFiles folder (WRQ) or the      ServerFiles folder (RRQ)
 
  2 --Access violation--
  In order to test this case edit a FILE located in 
	the clientFiles/ServerFiles, right click and select properties, go
	to the security tab and then to advanced, from there you should be able
	to disable inheritance, apply and then try to read/write the FILE
  
  3 --Disk full or allocation exceeded--
  Move/set up the project on a disk with under 1Mb and Write the "big" file to test WRQ
  Move "big" file from "./ClientFiles/" to "./ServerFiles/" to read and test RRQ
  
  6 --File already exists--
  In order to test the File Already Exists error, enter the name of a file that exists in the ClientFiles folder (RRQ) or the ServerFiles folder (WRQ).
*/
