#pragma once

extern "C" {
    #include "libavcodec/avcodec.h"
}

class H264Decoder {
private:
    AVCodecContext* codecCtx;
    AVFrame* m_Frame;
    AVCodecParserContext* parser;
    const AVCodec* codec;
    AVPacket* packet;

public:
    int init();
    int decode(unsigned char* data, int size, AVFrame** frame);
};