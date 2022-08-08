/************
Student Name: Dae Sung, Mukesh Rathore
File Name: ReadMe
Assignment Number: Project 2
************/

INSTRUCTIONS:
First compile the classes using javac command:

javac Network.java
javac Receiver.java
javac Sender.java

The format is following the instructions:

network: <port> <lostPercent> <delayedPercent> <errorPercent>
receiver: <port> 
sender: <port> <rcvHost> <rcvPort> <networkHost> <networkPort> 

example usage on Windows PowerShell in src/ folder

For ease of use, Please copy from below:

java Network 60086 20 20 20
java Receiver 60038
java Sender 60042 127.0.0.1 60038 127.0.0.1 60086

The Timeout is set for 500ms and 300ms for Network and Sender respectively so recommend using error corrupt lost percentages < 20

To get the full message from Receiver, type "shutdown" as input in Sender.

Thank you.