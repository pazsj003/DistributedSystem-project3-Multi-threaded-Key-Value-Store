There 5 argument for five port number :  
There 3 client jar and 3 server jar file for each scenario

Scenario One:
1) Run all 5 servers

2) Run client 1 and make it connect to server 1

3) Run client 2 and make it connect to server 2

4) Do put from client 1 on server 1

5) Do get from client 2 on server 2 -  We should be able to get the key put in from client 1
terminal:
java -jar client_s1.jar 1234 2345 3456 4567 5678
java -jar server_s1.jar 1234 2345 3456 4567 5678



Scenario Two:

1) Run all 5 servers

2) Run client 1 and make it connect to server 1

3) Bring down  server2

4) Do put from client on server1 - The operation should abort since server2 is down.

terminal:
java -jar client_s2.jar 1234 2345 3456 4567 5678
java -jar server_s2.jar 1234 2345 3456 4567 5678


Scenario Three:
Normal test 
terminal:
java -jar client_s3.jar 1234 2345 3456 4567 5678
java -jar server_s3.jar 1234 2345 3456 4567 5678



