//
// Created by mj on 18-12-21.
//

#ifndef MYTESTSERVICE_LHAVFORMAT_H
#define MYTESTSERVICE_LHAVFORMAT_H

#include "ffmpegHeader.h"

class LhAVFormat {
public:
    LhAVFormat();
    virtual ~LhAVFormat();

public:
    AVFormatContext* getInputFormatContext();
    AVFormatContext* getOutputFormatContext();

public:
    // only input file

    int open_input_file(const char* filename);
    int close_input_file();
    int find_video_stream();
    int find_audio_stream();
    AVCodecContext* getAudioCodecContext();
    AVCodecContext* getVideoCodecContext();

public:
    // only output file

    int open_output_file(const char *filename);
    AVStream* add_stream_by_stream(AVStream* pInStream, bool copyContext = false);
    AVStream* add_stream_by_codecContext(AVCodecContext* pCodecContext, bool copyContext = false);
    AVStream* add_stream_by_codec(const AVCodec* codec);
    int close_output_file();

public:
    // write method
    int read_packet(AVPacket* packet);

    int write_hearder();
    int write_mp4_header(bool faststart);
    int write_packet(AVPacket* packet);
    int write_interleave_packet(AVPacket* packet);
    int write_tailer();

private:
    AVFormatContext* m_pInputFormatContext;
    AVFormatContext* m_pOutputFormatContext;
};


#endif //MYTESTSERVICE_LHAVFORMAT_H
