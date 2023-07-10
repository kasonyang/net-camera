#pragma once

#pragma comment(lib,"Ws2_32.lib")

#include <winsock2.h>
#include <ws2tcpip.h>
#include <stdio.h>

class PacketReceiver {

private:

	SOCKET m_Socket;

	unsigned char* m_Data = nullptr;

	unsigned long m_DataLen = 0;


public:
	
	int init(PCSTR host, PCSTR port);

	int readPacket(unsigned char** data, unsigned long* size);

};