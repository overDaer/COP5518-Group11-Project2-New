COP5518-Group11-Project2
Dae Sung & Mukesh Rathore

First compile the classes using javac command:

javac Network.java
javac Sender.java
javac Receiver.java

The format is following the instructions:

receiver: <port> 
sender: <port> <rcvHost> <rcvPort> <networkHost> <networkPort> 
network: <port> <lostPercent> <delayedPercent> <errorPercent>

example usage on Windows PowerShell in src/ folder

For ease of use, please copy from below:

java Network 60086 20 20 20
java Sender 60042 127.0.0.1 60038 127.0.0.1 60086
java Receiver 60038

The Timeout is set for 500 ms and 300 ms for Network and Sender respectively so recommend using error corrupt lost percentages  <20

To get the full message from Receiver, type "shutdown" as input in Sender.

Thank you.