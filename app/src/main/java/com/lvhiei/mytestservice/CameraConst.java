package com.lvhiei.mytestservice;

/**
 * Created by mj on 16-2-19.
 */
public class CameraConst {
    public static class CameraSetting {
        public final static String PREV_CAMERA = "cameraSettings";
        public final static String PREV_WIDTH = "previewWidth";
        public final static String PREV_HEIGHT = "previewHeight";
    }

    public static class FuncMode{
        public static final int DEFAULT = 0x00;         // 默认
        public static final int RECORD_MV = 0x01;       // 录制mv
        public static final int K_ROOM = 0x02;       // 歌房
        public static final int LIVE = 0x04;       // 直播
    }
}
