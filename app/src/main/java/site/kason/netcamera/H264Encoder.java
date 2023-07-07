package site.kason.netcamera;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_frame;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_image_fill_arrays;
import static org.bytedeco.ffmpeg.global.avutil.av_opt_set;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR;
import static org.bytedeco.ffmpeg.global.swscale.sws_getCachedContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import site.kason.netcamera.util.Performance;

public class H264Encoder {

    private AVCodec encoder;
    private AVCodecContext codecCtx;

    private AVFrame tempFrame;
    private BytePointer tempFrameData;
    private long tempFrameDataLen;

    private AVFrame scaledFrame;
    private BytePointer scaledFrameData;
    private long scaledFrameDataLen;

    private SwsContext convertCtx;
    private AVPacket packet;

    private final Performance performance = new Performance();

    public void init(int width, int height, int[] preferredPixFmt) throws IOException {
        int bitRate = width * height * 3 / 2 * 16;
        int frameRate = 25;
        encoder = avcodec_find_encoder(AV_CODEC_ID_H264);
        if (encoder == null) {
            throw new IOException("encoder not found");
        }
        List<Integer> supportedFmts = getSupportPixelFormat(encoder, preferredPixFmt);
        for (Integer fmt : supportedFmts) {
            codecCtx = initCodecCtx(width, height, fmt, bitRate, frameRate);
            if (codecCtx != null) {
                break;
            }
        }
        if (codecCtx == null) {
            throw new IOException("Failed to initialize codec context");
        }
        tempFrame = av_frame_alloc();
        scaledFrame = av_frame_alloc();
        if (tempFrame == null || scaledFrame == null) {
            throw new OutOfMemoryError("failed to alloc frames");
        }
        tempFrame.pts(-1);
        packet = av_packet_alloc();
    }

    /**
     * @return 0=success and packet generated;AVERROR_EAGAIN=buffer not ready
     */
    public int recordFrame(Frame frame) {
        byte[] data = frame.data;
        int pf = frame.pixelFormat;
        if (tempFrameDataLen < data.length) {
            if (tempFrameData != null) {
                tempFrameData.releaseReference();
            }
            tempFrameData = new BytePointer(data.length);
            tempFrameDataLen = data.length;
        }
        tempFrameData.put(data);
        int width = frame.width;
        int height = frame.height;
        av_image_fill_arrays(tempFrame.data(), tempFrame.linesize(), tempFrameData, pf, width, height, frame.align);
        tempFrame.format(pf);
        tempFrame.width(width);
        tempFrame.height(height);
        tempFrame.pts(tempFrame.pts() + 1);
        return recordFrame(tempFrame);
    }

    /**
     * @return 0=success and packet generated;AVERROR_EAGAIN=buffer not ready
     */
    public int recordFrame(AVFrame frame) {
        int res = 0;
        int srcFmt = frame.format();
        int dstFmt = codecCtx.pix_fmt();
        int width = frame.width();
        int height = frame.height();
        if (srcFmt != dstFmt) {
            performance.begin("scale");
            convertCtx = sws_getCachedContext(
                    convertCtx,
                    width, height, srcFmt,
                    width, height, dstFmt,
                    SWS_BILINEAR, null, null, (DoublePointer) null
            );
            int requiredDataLen = width * height * 3 / 2;
            if (scaledFrameDataLen < requiredDataLen) {
                if (scaledFrameData != null) {
                    scaledFrameData.releaseReference();
                }
                scaledFrameData = new BytePointer(requiredDataLen);
                scaledFrameDataLen = requiredDataLen;
            }
            av_image_fill_arrays(scaledFrame.data(), scaledFrame.linesize(), scaledFrameData, dstFmt, width, height, 1);
            scaledFrame.format(dstFmt);
            scaledFrame.width(width);
            scaledFrame.height(height);
            scaledFrame.pts(frame.pts());
            res = sws_scale(convertCtx, frame.data(), frame.linesize(), 0, height, scaledFrame.data(), scaledFrame.linesize());
            if (res == 0) {
                throw new RuntimeException("scale frame failed");
            }
            frame = scaledFrame;
            performance.end("scale");
        }
        performance.begin("recordFrame");
        res = avcodec_send_frame(codecCtx, frame);
        scaledFrame.pts(scaledFrame.pts() + 1);
        if (res != 0 && res != AVERROR_EAGAIN()) {
            throw new RuntimeException("Failed to encode frame:" + res);
        }
        res = avcodec_receive_packet(codecCtx, packet);
        if (res != 0 && res != AVERROR_EAGAIN()) {
            return res;
        }
        performance.end("recordFrame");
        return res;
    }

    public AVPacket getPacket() {
        return packet;
    }

    public void cleanup() {
        if (codecCtx != null) {
            avcodec_free_context(codecCtx);
            codecCtx = null;
        }
        if (tempFrame != null) {
            av_frame_free(tempFrame);
        }
        if (scaledFrame != null) {
            av_frame_free(scaledFrame);
        }
        if (packet != null) {
            av_packet_free(packet);
        }
        //TODO clean m_ConvertCtx?
    }

    private AVCodecContext initCodecCtx(int width, int height,int pixFmt, int bitRate, int frameRate) {
        AVCodecContext codec_ctx = avcodec_alloc_context3(encoder);
        if (codec_ctx == null) {
            throw new OutOfMemoryError();
        }
        codec_ctx.codec_id(AV_CODEC_ID_H264);
        codec_ctx.pix_fmt(pixFmt);
        codec_ctx.width(width);
        codec_ctx.height(height);
        codec_ctx.bit_rate(bitRate);
        codec_ctx.rc_buffer_size(bitRate);
        codec_ctx.framerate().num(frameRate);
        codec_ctx.framerate().den(1);
        codec_ctx.gop_size(frameRate);//每秒1个关键帧
        codec_ctx.time_base().num(1);
        codec_ctx.time_base().den(frameRate);
        codec_ctx.has_b_frames(0);
        codec_ctx.global_quality(1);
        codec_ctx.max_b_frames(0);
        av_opt_set(codec_ctx.priv_data(), "tune", "zerolatency", 0);
        av_opt_set(codec_ctx.priv_data(), "preset", "ultrafast", 0);
        int ret = avcodec_open2(codec_ctx, encoder, (AVDictionary) null);
        return ret == 0 ? codec_ctx : null;
    }

    private static List<Integer> getSupportPixelFormat(AVCodec encoder, int... preferredFormats) {
        List<Integer> preferredList = new ArrayList<>(preferredFormats.length);
        for (int pf : preferredFormats) {
            preferredList.add(pf);
        }
        ArrayList<Integer> result = new ArrayList<>(8);
        IntPointer pf = encoder.pix_fmts();
        while (pf.get() != -1 && result.size() < 100) {
            result.add(pf.get());
            pf = pf.getPointer(1);
        }
        Collections.sort(result, (a, b) -> {
            int aIdx = preferredList.indexOf(a);
            int bIdx = preferredList.indexOf(b);
            if (aIdx >= 0 && bIdx >= 0) {
                return aIdx - bIdx;
            }
            return bIdx - aIdx;
        });
        return result;
    }

}
