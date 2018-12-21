//
// Created by mj on 18-12-21.
//

#include "LhAVCodec.h"

#include "logutil.h"

#define CHECK_CODEC(context, str) \
        if(!context){ \
            LOGE("%s : %s is not inited or set..", __func__, str); \
            return -1;\
        }

#define CHECK_ENCODER(context)  CHECK_CODEC(context, "encoder")
#define CHECK_DECODER(context)  CHECK_CODEC(context, "decoder")



LhAVCodec::LhAVCodec() {
    m_pEncoderCodecContext = nullptr;
    m_pDecoderCodecContext = nullptr;

    m_bAllocEncoder = false;
    m_bAllocDecoder = false;
}

LhAVCodec::~LhAVCodec() {
    if(m_pDecoderCodecContext){
        close_decoder();
    }

    if(m_pEncoderCodecContext){
        close_encoder();
    }
}

void LhAVCodec::setEncoderContext(AVCodecContext *codecContext) {
    m_pEncoderCodecContext = codecContext;
}

int
LhAVCodec::init_video_encoder(AVCodecID codecID, int width, int height, int framerate, int bitrate,
                              int gop, AVDictionary **param) {
    if(!m_pEncoderCodecContext){
        AVCodec* codec = avcodec_find_encoder(codecID);
        m_pEncoderCodecContext = avcodec_alloc_context3(codec);
        m_bAllocEncoder = true;
    }

    m_pEncoderCodecContext->codec_id = codecID;
    m_pEncoderCodecContext->codec_type = AVMEDIA_TYPE_VIDEO;

    //H264
    if(AV_CODEC_ID_H264 == m_pEncoderCodecContext->codec_id){
        av_dict_set(param, "preset", "ultrafast", 0);
        av_dict_set(param, "tune","zerolatency", 0);
        av_dict_set(param, "rc-lookahead", 0, 0);
        av_dict_set(param, "profile", "baseline", 0);
    }

    // copy 房间的设置
    m_pEncoderCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    m_pEncoderCodecContext->width = width;
    m_pEncoderCodecContext->height = height;
    m_pEncoderCodecContext->time_base.num = 1;
    m_pEncoderCodecContext->time_base.den = framerate;
    m_pEncoderCodecContext->level=40;

    m_pEncoderCodecContext->thread_count = 0;
    //Encoder parameters
    m_pEncoderCodecContext->refs = 1;
    m_pEncoderCodecContext->gop_size = gop ;

    //码流
    m_pEncoderCodecContext->bit_rate = bitrate;
    m_pEncoderCodecContext->rc_max_rate = m_pEncoderCodecContext->bit_rate*1.5;


    //disable B frame
    m_pEncoderCodecContext->max_b_frames = 0;
    m_pEncoderCodecContext->b_frame_strategy= 0;

    m_pEncoderCodecContext->trellis = 0;
    m_pEncoderCodecContext->qmin = 20;         //减少会较大的影响码流变化
    m_pEncoderCodecContext->qmax = 30;
    m_pEncoderCodecContext->max_qdiff = 10;

    m_pEncoderCodecContext->qcompress = 0.8;  //default 0.6
    m_pEncoderCodecContext->qblur = 0.5;                             //default 0.5  qblur=1 is a gaussian blur of radius 1.

    //熵编码
    m_pEncoderCodecContext->coder_type = FF_CODER_TYPE_AC;

    m_pEncoderCodecContext->flags |= CODEC_FLAG_GLOBAL_HEADER;

    LOGI("init_video_encoder codec:%d,width:%d,height:%d,fps:%d,bitrate:%d,gop:%d",
            codecID, width, height, framerate, bitrate, gop);
    return 0;
}

int LhAVCodec::init_audio_encoder(AVCodecID codecID, int samplerate, int channels, int bitrate,
                                  int profile) {
    if(!m_pEncoderCodecContext){
        AVCodec* codec = avcodec_find_encoder(codecID);
        m_pEncoderCodecContext = avcodec_alloc_context3(codec);
        m_bAllocEncoder = true;
    }
    
    m_pEncoderCodecContext->codec_id = codecID;
    m_pEncoderCodecContext->sample_fmt = AV_SAMPLE_FMT_S16;
    m_pEncoderCodecContext->sample_rate = samplerate;
    m_pEncoderCodecContext->channels = channels;
    m_pEncoderCodecContext->channel_layout = av_get_default_channel_layout(m_pEncoderCodecContext->channels);
    m_pEncoderCodecContext->bit_rate = bitrate;
    m_pEncoderCodecContext->codec_type = AVMEDIA_TYPE_AUDIO;
    m_pEncoderCodecContext->profile = profile;

    LOGI("init_audio_encoder codec:%d,samplerate:%d,channels:%d,bitrate:%d,profile:%d",
         codecID, samplerate, channels, bitrate, profile);
    return 0;
}

int LhAVCodec::open_encoder(AVDictionary *param) {
    CHECK_ENCODER(m_pEncoderCodecContext)

    int ret = 0;
    AVCodec* pCodec = avcodec_find_encoder(m_pEncoderCodecContext->codec_id);
    if(!pCodec){
        LOGE("VVAVEncoder::open_encoder find encoder failed, codec:%d", m_pEncoderCodecContext->codec_id);
        return -1;
    }

    m_pEncoderCodecContext->codec = pCodec;
    ret = avcodec_open2(m_pEncoderCodecContext, pCodec, &param);
    if(ret < 0){
        char buf[1024];
        av_strerror(ret, buf, 1024);
        LOGE("VVAVEncoder::open_encoder open codec failed,codec:%d,ret:%d(%s)",
             m_pEncoderCodecContext->codec_id, ret, buf);
        return ret;
    }

    return 0;
}

int LhAVCodec::close_encoder() {
    CHECK_ENCODER(m_pEncoderCodecContext)

    int ret = avcodec_close(m_pEncoderCodecContext);
    if(ret < 0){
        LOGE("close encoder error codec:%d,err:%d",
             m_pEncoderCodecContext->codec_id, ret);
    }

    if(m_bAllocEncoder){
        m_bAllocEncoder = false;
        avcodec_free_context(&m_pEncoderCodecContext);
    }
    m_pEncoderCodecContext = nullptr;
    return 0;
}

int LhAVCodec::encode_video_frame(AVPacket *packet, AVFrame *pFrame, int *got_frame) {
    CHECK_ENCODER(m_pEncoderCodecContext)

    int ret = 0;

    ret = avcodec_encode_video2(m_pEncoderCodecContext, packet, pFrame, got_frame);
    if(ret < 0){
        LOGE("VVAVEncoder::encode_video_frame failed,ret=%d", ret);
        return ret;
    }

//    av_frame_unref(pFrame);

    return 0;
}

int LhAVCodec::encode_audio_frame(AVPacket *packet, AVFrame *pFrame, int *got_frame) {
    CHECK_ENCODER(m_pEncoderCodecContext)

    int ret = 0;

    ret = avcodec_encode_audio2(m_pEncoderCodecContext, packet, pFrame, got_frame);
    if(ret < 0){
        LOGE("VVAVEncoder::encode_audio_frame failed,ret=%d", ret);
        return ret;
    }

//    av_frame_unref(pFrame);

    return 0;
}

int LhAVCodec::flush_video_encoder(AVPacket *packet, int *got_frame) {
    CHECK_ENCODER(m_pEncoderCodecContext)

    int ret = 0;
    ret = avcodec_encode_video2(m_pEncoderCodecContext, packet, NULL, got_frame);
    if (ret < 0) {
        LOGE("Error while flush_video_encoder, ret=%d", ret);
        return ret;
    }

    return 0;
}

int LhAVCodec::flush_audio_encoder(AVPacket *packet, int *got_frame) {
    CHECK_ENCODER(m_pEncoderCodecContext)

    int ret = 0;
    ret = avcodec_encode_audio2(m_pEncoderCodecContext, packet, NULL, got_frame);
    if (ret < 0) {
        LOGE("Error while flush_audio_encoder, ret=%d", ret);
        return ret;
    }

    return 0;
}

void LhAVCodec::setDecoderContext(AVCodecContext* codecContext) {
    m_pDecoderCodecContext = codecContext;
}

int LhAVCodec::open_decoder() {
    CHECK_DECODER(m_pDecoderCodecContext)

    int ret = 0;

    AVCodec* pCodec = avcodec_find_decoder(m_pDecoderCodecContext->codec_id);
    if(!pCodec){
        LOGE("VVAVDecoder::open_decoder, find decoder failed,decoder:%d",
             m_pDecoderCodecContext->codec_id);
        return -1;
    }

    ret = avcodec_open2(m_pDecoderCodecContext, pCodec, NULL);
    if(ret < 0){
        char buf[1024];
        av_strerror(ret, buf, 1024);
        LOGE("VVAVDecoder::open_decoder avcodec_open2 faield,codec:%d,ret=%d(%s)",
             m_pDecoderCodecContext->codec_id, ret, buf);
        return ret;
    }

    return 0;
}

int LhAVCodec::close_decoder() {
    CHECK_DECODER(m_pDecoderCodecContext)

    int ret = avcodec_close(m_pDecoderCodecContext);
    if(ret < 0){
        LOGE("close encoder error codec:%d,err:%d",
             m_pDecoderCodecContext->codec_id, ret);
    }

    return 0;
}

int LhAVCodec::decode_video(AVFrame *pFrame, int *got_picture, AVPacket *packet) {
    CHECK_DECODER(m_pDecoderCodecContext)

    int ret = 0;
    ret = avcodec_decode_video2(m_pDecoderCodecContext, pFrame, got_picture, packet);

    if(ret < 0){
        LOGE("avcodec_decode_video2 faield,ret:%d", ret);
        return ret;
    }

    av_packet_unref(packet);

    return 0;
}

int LhAVCodec::decode_audio(AVFrame *pFrame, int *got_picture, AVPacket *packet) {
    CHECK_DECODER(m_pDecoderCodecContext)

    int ret = 0;
    ret = avcodec_decode_audio4(m_pDecoderCodecContext, pFrame, got_picture, packet);

    if(ret < 0){
        LOGE("avcodec_decode_audio4 faield,ret:%d", ret);
        return ret;
    }

    av_packet_unref(packet);

    return 0;
}

int LhAVCodec::flush_video_decoder(AVFrame *pFrame, int *got_picture, AVPacket *packet) {
    CHECK_DECODER(m_pDecoderCodecContext)

    int ret = 0;
    ret = avcodec_decode_video2(m_pDecoderCodecContext, pFrame, got_picture, packet);
    if (ret < 0 ){
        return ret;
    }

    av_packet_unref(packet);
    return 0;
}

int LhAVCodec::flush_audio_decoder(AVFrame *pFrame, int *got_picture, AVPacket *packet) {
    CHECK_DECODER(m_pDecoderCodecContext)

    int ret = 0;
    ret = avcodec_decode_audio4(m_pDecoderCodecContext, pFrame, got_picture, packet);
    if (ret < 0 ){
        return ret;
    }

    av_packet_unref(packet);
    return 0;
}
