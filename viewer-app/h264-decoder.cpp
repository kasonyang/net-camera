#include "h264-decoder.h"

int H264Decoder::init() {
    codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (codec == nullptr) {
        printf("No H264 decoder found\n");
        return -1;
    }
    codecCtx = avcodec_alloc_context3(codec);
    codecCtx->flags |= AV_CODEC_FLAG_LOW_DELAY;
    if (avcodec_open2(codecCtx, codec, nullptr) < 0) {
        printf("Failed to open codec\n");
        return -2;
    }
    packet = av_packet_alloc();
    m_Frame = av_frame_alloc();
    parser = av_parser_init(AV_CODEC_ID_H264);
    return 0;
}

int H264Decoder::decode(unsigned char* data, int size, AVFrame** frame) {
    int new_pkg_ret = av_new_packet(packet, size);
    if (new_pkg_ret != 0) {
        printf("Failed to create new packet\n");
        return -1;
    }
    memcpy(packet->data, data, size);
    int ret = avcodec_send_packet(codecCtx, packet);
    if (ret < 0 && ret != AVERROR(EAGAIN)) {
        printf("Failed to parse packet\n");
        return -1;
    }
    ret = avcodec_receive_frame(codecCtx, m_Frame);
    if (ret == AVERROR(EAGAIN)) {
        *frame = nullptr;
        return 0;
    }
    if (ret != 0) {
        printf("Failed to read frame\n");
        return -1;
    }
    *frame = m_Frame;
    av_packet_unref(packet);
    return 0;
}