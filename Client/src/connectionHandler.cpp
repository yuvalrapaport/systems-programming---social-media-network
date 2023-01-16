#include <connectionHandler.h>
#include <vector>
#include <thread>
#include <boost/asio.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/lexical_cast.hpp>

 
using boost::asio::ip::tcp;
using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
 
ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(), socket_(io_service_){}
    
ConnectionHandler::~ConnectionHandler() {
    close();
}
 
bool ConnectionHandler::connect() {
    std::cout << "Starting connect to " 
        << host_ << ":" << port_ << std::endl;
    try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
 
bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
			tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);			
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
 
bool ConnectionHandler::getLine(std::string& line) {
    return getFrameAscii(line, '\n');
}

bool ConnectionHandler::sendLine(std::string& line) {
    return sendFrameAscii(line, '\n');
}
 
bool ConnectionHandler::getFrameAscii(std::string& frame, char delimiter) {
    char ch;
    // Stop when we encounter the null character. 
    // Notice that the null character is not appended to the frame string.
    try {
		do{
            if (!getBytes(&ch, 1))
                return false;

            frame.append(1, ch);
        }while (delimiter != ch);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    frame.append(" ");
    return true;
}
 
bool ConnectionHandler::sendFrameAscii(const std::string& frame, char delimiter) {
	bool result=sendBytes(frame.c_str(),frame.length());
	if(!result) return false;
	return sendBytes(&delimiter,1);
}
 
// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}

short ConnectionHandler::getOp(string str) {
    if (str.compare("REGISTER") == 0)
        return 1;
    if (str.compare("LOGIN") == 0)
        return 2;
    if (str.compare("LOGOUT") == 0)
        return 3;
    if (str.compare("FOLLOW") == 0)
        return 4;
    if (str.compare("POST") == 0)
        return 5;
    if (str.compare("PM") == 0)
        return 6;
    if (str.compare("LOGSTAT") == 0)
        return 7;
    if (str.compare("STAT") == 0)
        return 8;
    if (str.compare("NOTIFICATION") == 0)
        return 9;
    if (str.compare("ACK") == 0)
        return 10;
    if (str.compare("ERROR") == 0)
        return 11;
    if (str.compare("BLOCK") == 0)
        return 12;

    return -1;
}

short ConnectionHandler::bytesToShort(char* bytesArr)
{
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

void ConnectionHandler::shortToBytes(short num, char* bytesArr)
{
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

bool ConnectionHandler::sendEncodedLine(string& input){
    vector<char> temp;
    short op = 0;
    string str=input.substr(0,input.find(' '));
    input = input.substr(input.find(' ')+1);
    op = getOp(str);
    if (op == 0)
        return false;
    char opChars[2];
    shortToBytes(op,opChars);
    temp.push_back(opChars[0]);
    temp.push_back(opChars[1]);
    if ((op == 5) | (op ==8) | (op == 12)){
        for (char c : input)
            temp.push_back(c);
        temp.push_back('\0');
    }
    if ((op == 1) | (op == 2) | (op == 4) | (op == 6)){
        str=input.substr(0,input.find(' '));
        for (char c : str)
            temp.push_back(c);
        temp.push_back('\0');
    }
    if ((op == 6) | (op == 4)){
        input = input.substr(input.find(' ')+1);
        for (char c : input)
            temp.push_back(c);
        temp.push_back('\0');
    }
    if (op == 6)
    {
    	boost::posix_time::ptime timeLocal = boost::posix_time::second_clock::local_time();
    	string date =to_string(timeLocal.date().day())+'-'+to_string(timeLocal.date().month())+'-'+to_string(timeLocal.date().year())+' '+to_string(timeLocal.time_of_day().hours())+':'+to_string(timeLocal.time_of_day().minutes());
	for (char c : date)
            temp.push_back(c);
        temp.push_back('\0');
    }
    if ((op == 1) | (op == 2)){
        input=input.substr(input.find(' ')+1);
        str = input.substr(0, input.find(' '));
        for (char c : str)
            temp.push_back(c);
        temp.push_back('\0');
        input=input.substr(input.find(' ')+1);
        for (char c : input)
            temp.push_back(c);
        temp.push_back('\0');
    }
    temp.push_back(';');
    char output[temp.size()];
    int i = 0;
    for (char c : temp){
        output[i] = c;
        i++;
    }

    return sendBytes(output, temp.size());
}

bool ConnectionHandler::getDecodedLine(string& answer){
    char opBytes[2];
    char opMsg[2];

    try {
        if (!getBytes(opBytes,2))
            return false;
    }
    catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    short op = bytesToShort(opBytes);
    if ( op == 9 ) {
        answer.append("NOTIFICATION ");
        char notType[1];
        try {
            if (!getBytes(notType,1))
                return false;
        } catch (std::exception& e) {
            std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
            return false;
        }
        if (notType[0]=='0')
            answer.append("PM ");
        else
            answer.append("Public ");

        string temp;
        if (getFrameAscii(temp,'\0') && getFrameAscii(temp,'\0'))
            answer.append(temp);
        else
            return false;
    }
    else if ( op == 10) {
        answer.append("ACK ");
        try {
            if (!getBytes(opMsg, 2))
                return false;
        } catch (std::exception& e) {
            std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
            return false;
        }
        short msgType = bytesToShort(opMsg);
        answer.append(to_string(msgType)+" ");
        if (msgType == 4){
            string temp;
            if (getFrameAscii(temp,'\0'))
                answer.append(temp);
            else
                return false;
        }
        else if ((msgType == 7) | (msgType == 8)){
            for (int i =0; i<4; i++){
                char next[2];
                try {
                    if (!getBytes(next,2))
                        return false;
                } catch (std::exception& e) {
                    std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
                    return false;
                }
                answer.append(to_string((bytesToShort(next)))+ " ");
            }
        }
    }
    else if (op == 11) {
        answer.append("ERROR ");
        try {
            if (!getBytes(opMsg, 2))
                return false;
        } catch (std::exception& e) {
            std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
            return false;
        }
        short msgType = bytesToShort(opMsg);
        answer.append(to_string(msgType));
    }
//    else
//        return false;
    return true;
}
