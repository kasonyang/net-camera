// net-camera-viewer.cpp : Defines the entry point for the application.
//

#include "net-camera-viewer.h"

#include "SDL.h"
#include "packet-receiver.h"
#include "h264-decoder.h"
#include "yuv-render.h"
#include "performance.h"

int main(int argc, char* argv[]) {
    if (argc < 2) {
        printf("Usagge: net-camera-viewer <IP> <PORT>\n");
        return -1;
    }
    PCSTR host = argv[0];
    PCSTR port = argv[1];
    Performance* performance = new Performance(100);
    PacketReceiver* pkgReceiver = new PacketReceiver();
    H264Decoder* decoder = new H264Decoder();
    YuvRender* yuvRender = nullptr;
    if (pkgReceiver->init(host, port) != 0) {
        return -1;
    }
    if (decoder->init() != 0) {
        return -2;
    }
    unsigned char* pkgData;
    unsigned long pkgLen;
    AVFrame* frame;
    for (;;) {
        if (pkgReceiver->readPacket(&pkgData, &pkgLen) != 0) {
            return -3;
        }
        performance->begin("decode");
        if (decoder->decode(pkgData, pkgLen, &frame) != 0) {
            return -4;
        }
        performance->end("decode");
        if (frame == nullptr) {
            continue;
        }
        // printf("receive frame:%d\n", frame);
        if (yuvRender == nullptr) {
            printf("video size: %d x %d\n", frame->width, frame->height);
            yuvRender = new YuvRender();
            if (yuvRender->init(frame->width, frame->height) != 0) {
                printf("Failed to init render\n");
                return -5;
            }
        }
        performance->begin("render");
        yuvRender->render(frame->data, frame->linesize);
        performance->end("render");
    }
}
