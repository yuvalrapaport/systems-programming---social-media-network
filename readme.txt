1.1) TPC server:
	mvn clean
	mvn compile
	mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.BGSServer.TPCMain" -Dexec.args="7777"
 reactor server:
 	mvn clean
	mvn compile
	mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.BGSServer.ReactorMain" -Dexec.args="7777 5"

Client:
	make
	./bin/BGSClient 127.0.0.1 7777
	
1.2) 
	REGISTER username password DD-MM-YYYY
	LOGIN username password 1/0
	LOGOUT
	FOLLOW 0/1 username
	POST content
	PM username content
	LOGSTAT
	STAT username1|username2
	BLOCK username

2) the list of filtered words is in: /spl-net/src/main/java/bgu/spl/net/impl/BGSServer/BidiMessagingProtocolImpl at the function "filter"
