#include "packet-receiver.h"

bool read_data(SOCKET socket, void* data, unsigned int len) {
    while (len > 0) {
        int ret = recv(socket, (char*)data, len, 0);
        if (ret <= 0) {
            return false;
        }
        len -= ret;
        data = (char*)data + ret;
    }
    return true;
}

bool read_ushort(SOCKET socket, unsigned short* value) {
    bool ret = read_data(socket, value, 2);
    if (ret) {
        *value = ntohs(*value);
    }
    return ret;
}

bool read_int(SOCKET socket, ULONG* value) {
    bool ret = read_data(socket, value, 4);
    if (ret) {
        *value = ntohl(*value);
    }
    return ret;
}

int PacketReceiver::init(PCSTR host, PCSTR port) {
	WORD sockVersion = MAKEWORD(2, 2);
	WSADATA wsaData;

	if (WSAStartup(sockVersion, &wsaData) != 0) {
		printf("WSAStartup() error!\n");
		return -1;
	}
    SOCKET sclient = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (sclient == INVALID_SOCKET)
    {
        printf("invalid socket !");
        return -2;
    }

    struct sockaddr_in serAddr;
    serAddr.sin_family = AF_INET;
    serAddr.sin_port = htons(6666);
    serAddr.sin_addr.S_un.S_addr = inet_addr("127.0.0.1");
    if (connect(sclient, (struct sockaddr*)&serAddr, sizeof(serAddr)) == SOCKET_ERROR)
    {
        printf("connect error !");
        closesocket(sclient);
        return -3;
    }

	m_Socket = sclient;
    return 0;

}

int PacketReceiver::readPacket(unsigned char** data, unsigned long* size) {
    ULONG pkgSize = 0;
    bool ret = read_int(m_Socket, &pkgSize);
    if (!ret) {
        printf("Failed to read packet size\n");
        return -1;
    }
    if (m_DataLen < pkgSize) {
        if (m_Data != nullptr) {
            delete[] m_Data;
        }
        m_Data = new unsigned char[pkgSize];
        m_DataLen = pkgSize;
    }
    if (!read_data(m_Socket, m_Data, pkgSize)) {
        printf("Failed to read packet data\n");
        return -2;
    }
    *data = m_Data;
    *size = pkgSize;
    return 0;
}