//
// Created by mj on 18-12-21.
//

#include "LhAVFormat.h"
#include "logutil.h"

#define CHECK_FILE_OPEN(context, str, ret) \
        if(!context){ \
            LOGE("%s : %s file not open..", __func__, str); \
            return ret;\
        }

#define CHECK_OUT_FILE_OPEN(context, ret)   CHECK_FILE_OPEN(context, "output", ret)
#define CHECK_IN_FILE_OPEN(context, ret)    CHECK_FILE_OPEN(context, "input", ret)

#define CHECK_OUT_FILE_OPEN_INT(context)    CHECK_OUT_FILE_OPEN(context, -1)
#define CHECK_OUT_FILE_OPEN_NULL(context)   CHECK_OUT_FILE_OPEN(context, nullptr)

#define CHECK_IN_FILE_OPEN_INT(context)     CHECK_IN_FILE_OPEN(context, -1)
#define CHECK_IN_FILE_OPEN_NULL(context)    CHECK_IN_FILE_OPEN(context, nullptr)

LhAVFormat::LhAVFormat() {
    m_pInputFormatContext = nullptr;
    m_pOutputFormatContext = nullptr;
}

LhAVFormat::~LhAVFormat() {
    close_input_file();
    close_output_file();
}

AVFormatContext *LhAVFormat::getInputFormatContext() {
    return m_pInputFormatContext;
}

AVFormatContext *LhAVFormat::getOutputFormatContext() {
    return m_pOutputFormatContext;
}

int LhAVFormat::open_input_file(const char *filename) {
    if(m_pInputFormatContext){
        close_input_file();
    }

    int ret = 0;

    m_pInputFormatContext = avformat_alloc_context();

    ret = avformat_open_input(&m_pInputFormatContext, filename, NULL, NULL);
    if(ret != 0){
        LOGE("avformat_open_input failed ret=%d,filename=%s", ret, filename);
        return ret;
    }

    ret = avformat_find_stream_info(m_pInputFormatContext, NULL);
    if(ret < 0){
        LOGE("avformat_find_stream_info failed ret=%d", ret);
        return ret;
    }

    return 0;
}

int LhAVFormat::close_input_file() {
    if(m_pInputFormatContext)
    {
        avformat_close_input(&m_pInputFormatContext);
        avformat_free_context(m_pInputFormatContext);
        m_pInputFormatContext = nullptr;
    }

    return 0;
}

int LhAVFormat::find_video_stream() {
    CHECK_IN_FILE_OPEN_INT(m_pInputFormatContext)

    int videoindex = -1;
    for(int i = 0; i < m_pInputFormatContext->nb_streams; i++)
    {
        if(m_pInputFormatContext->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
        {
            videoindex = i;
            break;
        }
    }

    return videoindex;
}

int LhAVFormat::find_audio_stream() {
    CHECK_IN_FILE_OPEN_INT(m_pInputFormatContext)
    int audioindex = -1;
    for(int i = 0; i < m_pInputFormatContext->nb_streams; i++)
    {
        if(m_pInputFormatContext->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            audioindex = i;
            break;
        }
    }

    return audioindex;
}

AVCodecContext *LhAVFormat::getAudioCodecContext() {
    CHECK_IN_FILE_OPEN_NULL(m_pInputFormatContext)

    int idx = find_audio_stream();
    if(idx < 0){
        return nullptr;
    }

    return m_pInputFormatContext->streams[idx]->codec;
}

AVCodecContext *LhAVFormat::getVideoCodecContext() {
    CHECK_IN_FILE_OPEN_NULL(m_pInputFormatContext)

    int idx = find_video_stream();
    if(idx < 0){
        return nullptr;
    }

    return m_pInputFormatContext->streams[idx]->codec;
}

int LhAVFormat::open_output_file(const char *filename) {
    if(m_pOutputFormatContext){
        close_output_file();
    }

    int ret = 0;
    //申请输出文件内存
    m_pOutputFormatContext = avformat_alloc_context();
    ret = avformat_alloc_output_context2(&m_pOutputFormatContext, NULL, NULL, filename);
    if (!m_pOutputFormatContext) {
        LOGE("avformat_alloc_output_context2 failed ret=%d,filename=%s", ret, filename);
        return ret;
    }

    AVOutputFormat* ofmt = m_pOutputFormatContext->oformat;

//    av_dump_format(pFormatContext, 0, filename, 1);

    //打开输出文件
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&m_pOutputFormatContext->pb, m_pOutputFormatContext->filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("avio_open failed ret=%d,filename=%s", ret, m_pOutputFormatContext->filename);
            return ret;
        }
    }

    return 0;
}

AVStream *LhAVFormat::add_stream_by_stream(AVStream *pInStream, bool copyContext) {
    if(!pInStream){
        return nullptr;
    }

    AVStream* out_stream = add_stream_by_codecContext(pInStream->codec, copyContext);

    if(out_stream && pInStream){
        out_stream->time_base = pInStream->time_base;
    }

    return out_stream;
}

AVStream *LhAVFormat::add_stream_by_codecContext(AVCodecContext *pCodecContext, bool copyContext) {
    if(!pCodecContext){
        return nullptr;
    }

    int ret = 0;
    AVStream *out_stream = add_stream_by_codec(pCodecContext->codec);

    //Copy the settings of AVCodecContext
    if(copyContext && pCodecContext){
        ret = avcodec_copy_context(out_stream->codec, pCodecContext);
        if(ret < 0){
            LOGE("avcodec_copy_context failed ret=%d", ret);
        }
    }

    out_stream->codec->codec_tag = 0;
    if (m_pOutputFormatContext->oformat->flags & AVFMT_GLOBALHEADER){
        out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    return out_stream;
}

AVStream *LhAVFormat::add_stream_by_codec(const AVCodec *codec) {
    CHECK_OUT_FILE_OPEN_NULL(m_pOutputFormatContext)

    if(!codec){
        return nullptr;
    }

    AVStream *out_stream = avformat_new_stream(m_pOutputFormatContext, codec);
    if (!out_stream) {
        LOGE("avformat_new_stream failed");
        return nullptr;
    }

    return out_stream;
}

int LhAVFormat::close_output_file() {
    int ret = 0;

    if (m_pOutputFormatContext && !(m_pOutputFormatContext->oformat->flags & AVFMT_NOFILE)){
        ret = avio_close(m_pOutputFormatContext->pb);

        if(ret < 0){
            LOGE("avio_close failed ret=%d", ret);
            return ret;
        }

        avformat_free_context(m_pOutputFormatContext);
    }

    return 0;
}

int LhAVFormat::read_packet(AVPacket *packet) {
    CHECK_OUT_FILE_OPEN_INT(m_pOutputFormatContext)

    return av_read_frame(m_pInputFormatContext, packet);
}

int LhAVFormat::write_hearder() {
    CHECK_OUT_FILE_OPEN_INT(m_pOutputFormatContext)

    int ret = avformat_write_header(m_pOutputFormatContext, NULL);
    if (ret < 0) {
        LOGE("avformat_write_header failed ret=%d", ret);
        return ret;
    }

    return 0;
}

int LhAVFormat::write_mp4_header(bool faststart) {
    CHECK_OUT_FILE_OPEN_INT(m_pOutputFormatContext)


    AVDictionary* opt = NULL;
    if(!strcmp(m_pOutputFormatContext->oformat->name, "mp4") && faststart){
        av_dict_set_int(&opt, "movflags", FF_MOV_FLAG_FASTSTART, 0);
    }

    int ret = avformat_write_header(m_pOutputFormatContext, &opt);
    //Write file header
    if (ret < 0) {
        LOGE("avformat_write_header mp4 failed ret=%d,faststart=%d", ret, faststart);
        return ret;
    }

    return 0;
}

int LhAVFormat::write_packet(AVPacket *packet) {
    CHECK_OUT_FILE_OPEN_INT(m_pOutputFormatContext)

    int ret = av_write_frame(m_pOutputFormatContext, packet);
    if(ret < 0){
        LOGE("av_write_frame failed ret=%d", ret);
        return ret;
    }

    return 0;
}

int LhAVFormat::write_interleave_packet(AVPacket *packet) {
    CHECK_OUT_FILE_OPEN_INT(m_pOutputFormatContext)

    int ret = av_interleaved_write_frame(m_pOutputFormatContext, packet);
    if(ret < 0){
        LOGE("av_interleaved_write_frame failed ret=%d", ret);
        return ret;
    }

    return 0;
}

int LhAVFormat::write_tailer() {
    CHECK_OUT_FILE_OPEN_INT(m_pOutputFormatContext)

    int ret = av_write_trailer(m_pOutputFormatContext);
    if(ret != 0){
        LOGE("av_write_trailer failed ret=%d", ret);
        return ret;
    }

    return 0;
}
