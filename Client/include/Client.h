//
// Created by rapapory@wincs.cs.bgu.ac.il on 27/12/2021.
//

#ifndef CLIENT_CLIENT_H
#define CLIENT_CLIENT_H
#include <thread>
#include <mutex>
#include <condition_variable>
#include <iostream>
#include "connectionHandler.h"

using namespace std;

class Client{
private:
    string _host;
    short _port;
    bool shouldTerminate;
    ConnectionHandler connectionHandler;
    thread* readFromServer;
    mutex& _mutex;
    condition_variable cv;

public:
    Client(string host, short port, mutex& mutex);
    virtual ~Client(); //destructor

    Client(const Client& other); //copy constructor
    Client(Client&& other); //move constructor
    Client& operator=(const Client& other); //assignment operator
    Client& operator=(const Client&& other); //move assignment operator


    void readFromKeyboard();
    void runNetworkRead(ConnectionHandler* CH);
};

#endif //CLIENT_CLIENT_H
