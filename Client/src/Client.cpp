#include <stdlib.h>
#include <connectionHandler.h>
#include "Client.h"

using namespace std;

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/

Client::Client(string host, short port, mutex& mutex):_host(host),_port(port),shouldTerminate(false),connectionHandler(_host,_port),readFromServer(),_mutex(mutex),cv(){} //constructor
Client::~Client(){} //destructor
Client::Client(const Client& other):_host(other._host),_port(other._port),shouldTerminate(other.shouldTerminate),connectionHandler(other._host,other._port),readFromServer(),_mutex(other._mutex),cv(){} //copy constructor
Client::Client(Client&& other):_host(other._host),_port(other._port),shouldTerminate(other.shouldTerminate),connectionHandler(other._host,other._port),readFromServer(),_mutex(other._mutex),cv(){} //move constructor
Client& Client::operator=(const Client& other){ return *this;} //assignment operator
Client& Client::operator=(const Client&& other){return *this; } //move assignment operator

void Client::runNetworkRead(ConnectionHandler* connectionHandler){
    while (!shouldTerminate){
        string answer;
        if (!connectionHandler->getDecodedLine(answer)){
            cout << "Disconnected" << endl;
            shouldTerminate = true;
            break;
        }
        if(!answer.empty())
            cout << answer << endl;
        if (answer == "ACK 3 "){
            shouldTerminate = true;
            unique_lock<mutex> lock(_mutex);
            cv.notify_all();
        }
        if (answer == "ERROR 3"){
            unique_lock<mutex> lock(_mutex);
            cv.notify_all();
        }
    }
}

void Client::readFromKeyboard(){
    shouldTerminate = false;
    if (!connectionHandler.connect()){
        cout << "cannot connect to server" << endl;
        shouldTerminate = true;
    }
    else
        readFromServer = new thread(&Client::runNetworkRead,this,&connectionHandler);
    while (!shouldTerminate){
        const short bufsize = 1024;
        char buf[bufsize];
        cin.getline(buf, bufsize);
        string line(buf);
        if (!connectionHandler.sendEncodedLine(line)){
            cout << "Disconnected" << endl;
            shouldTerminate = true;
            break;
        }
        if (line == "LOGOUT"){
            unique_lock<mutex> lock(_mutex);
            cv.wait(lock);
        }
    }
    readFromServer->join();
    delete readFromServer;
}

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }

    std::string host = argv[1];
    short port = atoi(argv[2]);
    mutex mutex;
    Client* client = new Client(host, port, mutex);
    client->readFromKeyboard();
    delete client;

}
