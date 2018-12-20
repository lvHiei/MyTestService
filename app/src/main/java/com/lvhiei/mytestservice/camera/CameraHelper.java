package com.lvhiei.mytestservice.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;

import com.lvhiei.mytestservice.MyLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by mj on 16-2-23.
 */
public class CameraHelper {

    private static final int DEFAULT_CAMERA_FPS = 25;
    private static final int DEFAULT_MAX_CAMERA_SIZE = 1280*720;
    private static final int CAMERA_PREVIEW_960 = 960;
    private static final int CAMERA_PREVIEW_540  = 540;
    private static final int CAMERA_PREVIEW_544  = 544; // 这里有的手机支持540,有的手机支持544

    private static final int CAMERA_PREVIEW_1280 = 1280;
    private static final int CAMERA_PREVIEW_720 = 720;
    private static final int CAMERA_PREVIEW_640 = 640;
    private static final int CAMERA_PREVIEW_480 = 480;

    private static final float FLOAT_PRECITION = 0.001f;

    protected MyLog _log = new MyLog(this.getClass().getName());

    private int m_iCameraWidth;
    private int m_iCameraHeight;
    private int m_iCameraFps = DEFAULT_CAMERA_FPS;
    private boolean m_bFlashOff = true;


    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private Context m_context;
    private Camera m_camera = null;
    private boolean m_bFrontCamera = true;
    private final Comparator m_compare = new SortCameraSize();
    private SharedPreferences m_spCameraSet;
    private int m_iCameraNum = 0;
    private boolean m_bCameraOpened = false;

    private byte[][] mPreviewBuffers = null;
    private static final int mPreviewBufferCount = 4;
    private long mPreviewBufferSize = 0;
    private static boolean msbLittleCameraResulotion = false;
    private static boolean msbDisableFrameRate = false;

    private List<Camera.Size> mSupportedSize;

    private int mExpectPreviewWidth;
    private int mExpectPreviewHeight;

    private int mFuncCode;

    private boolean mbCameraSwitched = false;
    private boolean mbCameraPreivewSizeChanged = false;

    private class SortCameraSize implements Comparator {
        @Override
        public int compare(Object left, Object right) {
            Camera.Size leftSize = (Camera.Size) left;
            Camera.Size rightSize = (Camera.Size) right;
            int LeftNum = leftSize.width * leftSize.height;
            int RightNum = rightSize.width * rightSize.height;
            if (LeftNum > RightNum)
                return -1;
            else if (LeftNum < RightNum)
                return 1;
            else {
                return 0;
            }
        }
    }

    public CameraHelper(Context context){
        if(null == context){
            return;
        }

        m_context = context.getApplicationContext();

        if(null != m_context){
            m_spCameraSet = m_context.getSharedPreferences(CameraConst.CameraSetting.PREV_CAMERA, Context.MODE_PRIVATE);
            if (null != m_spCameraSet){
                m_bFrontCamera = m_spCameraSet.getBoolean(CameraConst.CameraSetting.PREV_CAMERA, true);
            }
        }

        mExpectPreviewWidth = CAMERA_PREVIEW_960;
        mExpectPreviewHeight = CAMERA_PREVIEW_540;
        mFuncCode = CameraConst.FuncMode.DEFAULT;
    }

    public static void setLittleCameraResulotion(boolean littleCameraResulotion){
        msbLittleCameraResulotion = littleCameraResulotion;
    }

    public static void setDisableCameraFramerate(boolean disableCameraFramerate){
        msbDisableFrameRate = disableCameraFramerate;
    }

    public void setFuncCode(int funcCode){
        mFuncCode = funcCode;
        // 歌房记忆preview大小
        if(mFuncCode == CameraConst.FuncMode.K_ROOM){
            if(m_spCameraSet != null){
                mExpectPreviewWidth = m_spCameraSet.getInt(CameraConst.CameraSetting.PREV_WIDTH, CAMERA_PREVIEW_640);
                mExpectPreviewHeight = m_spCameraSet.getInt(CameraConst.CameraSetting.PREV_HEIGHT, CAMERA_PREVIEW_480);
            }
        }
    }

    public boolean isOpened(){
        return m_bCameraOpened;
    }

    public Camera openCamera(){
        closeCameraWithoutFreePreviewBuffer();
        if (null == m_camera){
            try{
                _log.i("opening camera ");
                if (m_bFrontCamera){
                    mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    m_camera = getFrontCameraInstance();
                }else{
                    mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
                    m_camera = getBackCameraInstance();
                }

                if(null == m_camera){
                    if (m_bFrontCamera){
                        mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
                        m_camera = getBackCameraInstance();
                        m_bFrontCamera = false;
                    }else{
                        mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
                        m_camera = getFrontCameraInstance();
                        m_bFrontCamera = true;
                    }
                }

                if(null == m_camera){
                    _log.e("get camera instance failed!");
                    return null;
                }

                m_camera.getCameraInfo(mCameraID, mCameraInfo);

                Camera.Parameters parameters = m_camera.getParameters();

                setPreviewSize(parameters);

                if(!msbDisableFrameRate){
                    int fps = getBestCameraFps(parameters.getSupportedPreviewFrameRates(), DEFAULT_CAMERA_FPS);
                    m_iCameraFps = fps;
                    parameters.setPreviewFrameRate(m_iCameraFps);
                    _log.i("majun_test:setPreviewFrameRate : %d", m_iCameraFps);
                }

                parameters.setPreviewFormat(ImageFormat.NV21);

                int exposure = getBestExplosure(parameters.getMinExposureCompensation(), parameters.getMaxExposureCompensation());

                parameters.setExposureCompensation(exposure);

                if(!m_bFrontCamera){
//                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    List<String> focuses = parameters.getSupportedFocusModes();
                    if (focuses.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                }

                resetPreviewBuffers();

                m_camera.setParameters(parameters);

                _log.i("open camera success");
            }catch (RuntimeException e){
                closeCamera();
                _log.e("open camera failed");
                e.printStackTrace();
            }
        }

        m_bCameraOpened = true;

        return m_camera;
    }

    public boolean startPreview(SurfaceTexture surfaceTexture, Camera.PreviewCallback callback) throws IOException {
        if(null == m_camera){
            openCamera();
        }

        if(null == m_camera){
            return false;
        }

        _log.i("camera startPreview...");
        for(int i = 0; i < mPreviewBufferCount; ++i){
            m_camera.addCallbackBuffer(mPreviewBuffers[i]);
        }
        m_camera.setPreviewCallbackWithBuffer(callback);
        m_camera.setPreviewTexture(surfaceTexture);
//        m_camera.setDisplayOrientation(90);
        m_camera.startPreview();
        return true;
    }

    public boolean stopPreview(){
        if(null != m_camera){
            try{
                _log.i("camera stopPreview...");
                m_camera.setPreviewCallback(null);
                m_camera.stopPreview();
            }catch (RuntimeException e){
                return false;
            }
        }

        return true;
    }

    public Camera switchCamera(){
        if (1 == m_iCameraNum && null != m_camera){
            return m_camera;
        }

        closeCameraWithoutFreePreviewBuffer();
        m_bFrontCamera = !m_bFrontCamera;

        if(m_bFrontCamera){
            m_bFlashOff = true;
        }

        mbCameraSwitched = true;
        return openCamera();
    }

    public void changePreivewSize(int width, int height){
        if(mExpectPreviewWidth == width && mExpectPreviewHeight == height){
            return;
        }

        closeCameraWithoutFreePreviewBuffer();
        mExpectPreviewWidth = width;
        mExpectPreviewHeight = height;
        openCamera();

        mbCameraPreivewSizeChanged = true;
    }

    public List<Camera.Size> getSupportedPreviewSize(){
        return mSupportedSize;
    }


    public void closeCameraWithoutFreePreviewBuffer(){
        if(null != m_camera){
            try{
                m_bCameraOpened = false;
                m_camera.setPreviewCallback(null);
                m_camera.stopPreview();
                m_camera.release();
                m_bFlashOff = true;
                m_camera = null;
            }catch (RuntimeException e){
                e.printStackTrace();
            }
        }
    }

    public void closeCamera(){
        if(null != m_camera){
            try{
                _log.i("closing camera ");
                m_bCameraOpened = false;
                m_camera.setPreviewCallback(null);
                m_camera.stopPreview();
                m_camera.release();
                m_camera = null;
                mPreviewBuffers = null;
                m_bFlashOff = true;
                saveSP();
                _log.i("close camera success");
            }catch (RuntimeException e){

            }
        }
    }

    public void setMeteringArea(Rect rect, int weight){
        if(null == m_camera){
            return;
        }

        try {
            Camera.Parameters parameters = m_camera.getParameters();
            if(parameters.isAutoExposureLockSupported()){
                if(parameters.getAutoExposureLock()){
                    parameters.setAutoExposureLock(false);
                }
//                return;
            }

            int maxMeteringAreas = parameters.getMaxNumMeteringAreas();
            if(maxMeteringAreas <= 0){
                _log.i("setMeteringArea failed, maxMeteringArea is %d", maxMeteringAreas);
                return;
            }

            _log.i("setMeteringArea rect:%s, weight:%d", rect.toString(), weight);

            List<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(rect, weight));
            parameters.setMeteringAreas(areas);
            m_camera.setParameters(parameters);
        }catch (Exception e){
        }
    }

    public int getPreviewBufferCount(){
        return mPreviewBufferCount;
    }

    public int getCameraID(){
        return mCameraID;
    }

    public int getOrientation(){
        if(mCameraInfo == null){
            return 0;
        }
        return mCameraInfo.orientation;
    }

    public int getCameraWidth(){
        return m_iCameraWidth;
    }

    public int getCameraHeight(){
        return m_iCameraHeight;
    }

    public int getCameraFps(){
        return m_iCameraFps;
    }

    public boolean isFrontCamera(){
        return m_bFrontCamera;
    }


    public void switchFlash() {
        if (m_bFlashOff) {
            turnOnFlash();
        } else {
            turnOffFlash();
        }
    }

    private void turnOnFlash() {
        if (null == m_camera) {
            return;
        }

        if (m_bFrontCamera) {
            return;
        }

        Camera.Parameters parameters = m_camera.getParameters();
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            // Use the screen as a flashlight (next best thing)
            return;
        }

        String flashMode = parameters.getFlashMode();
        if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                m_camera.setParameters(parameters);
                m_bFlashOff = false;
            }
        }
    }

    private void turnOffFlash() {
        if (null == m_camera) {
            return;
        }

        if (m_bFrontCamera) {
            return;
        }

        Camera.Parameters parameters = m_camera.getParameters();
        if (null == parameters) {
            return;
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        String flashMode = parameters.getFlashMode();

        if (null == flashModes) {
            return;
        }

        if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                m_camera.setParameters(parameters);
                m_bFlashOff = true;
            }
        }
    }

    public boolean onFocus(Rect focusRect, Camera.AutoFocusCallback callback) {
        if (m_camera == null) {
            return false;
        }

        Camera.Parameters parameters = null;
        try {
            parameters = m_camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //不支持设置自定义聚焦，则使用自动聚焦，返回

        if (Build.VERSION.SDK_INT >= 14) {

            try {
                if (parameters.getMaxNumFocusAreas() <= 0) {
                    _log.i("onCameraFocus: MaxNumFocusAreas <= 0");
                    return autoFocus(callback);
                }

                _log.i("onCameraFocus:" + focusRect.left + "," + focusRect.right + ";" + focusRect.top
                        + "," + focusRect.bottom);

                //定点对焦
                List<Camera.Area> areas = new ArrayList<Camera.Area>();
                areas.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusMode("auto");
                parameters.setFocusAreas(areas);
//            parameters.setMeteringAreas(areas);
                m_camera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return autoFocus(callback);
    }

    private boolean autoFocus(Camera.AutoFocusCallback callback) {
        _log.i("onCameraFocus: to Auto Focus");
        try {
            m_camera.autoFocus(callback);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int getZoomValue() {
        if (null == m_camera) {
            return 0;
        }
        Camera.Parameters parameters = null;
        try {
            parameters = m_camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        try {
            return parameters.getZoom();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getMaxZoomValue() {
        if (null == m_camera) {
            return 0;
        }
        Camera.Parameters parameters = null;
        try {
            parameters = m_camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        try {
            return parameters.getMaxZoom();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean setZoom(int value) {
        if (null == m_camera) {
            return false;
        }
        Camera.Parameters parameters = null;
        try {
            parameters = m_camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try {
            int maxValue = parameters.getMaxZoom();
            if (maxValue <= 0)
                return false;

            int zoomValue = value;
            if (zoomValue > parameters.getMaxZoom() || zoomValue < 0)
                return false;
            parameters.setZoom(zoomValue);
            m_camera.setParameters(parameters);
            _log.i("setZoom zoomValue : " + zoomValue);
            return parameters.isZoomSupported();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public int getMaxExposureCompensation(){
        if(null == m_camera){
            return 0;
        }

        Camera.Parameters parameters = m_camera.getParameters();
        return parameters.getMaxExposureCompensation();
    }

    public int getMinExposureCompensation(){
        if(null == m_camera){
            return 0;
        }

        Camera.Parameters parameters = m_camera.getParameters();
        return parameters.getMinExposureCompensation();
    }

    public void setExposureCompensation(int exposureCompensation){
        if(null == m_camera){
            return;
        }

        Camera.Parameters parameters = m_camera.getParameters();

        if(exposureCompensation < parameters.getMinExposureCompensation()){
            exposureCompensation = parameters.getMinExposureCompensation();
        }

        if(exposureCompensation > parameters.getMaxExposureCompensation()){
            exposureCompensation = parameters.getMaxExposureCompensation();
        }

        parameters.setExposureCompensation(exposureCompensation);
        m_camera.setParameters(parameters);
    }

    public void setExposureCompensationByPercent(int percent){
        if(null == m_camera){
            return;
        }

        Camera.Parameters parameters = m_camera.getParameters();

        if(percent < 0){
            percent = 0;
        }

        if(percent > 100){
            percent = 100;
        }

        int max = parameters.getMaxExposureCompensation();
        int min = parameters.getMinExposureCompensation();

        int exposureCompensation = (int) (1.0f * percent * (max - min) / 100) + min;

        parameters.setExposureCompensation(exposureCompensation);
        m_camera.setParameters(parameters);
    }

    private void setPreviewSize(Camera.Parameters parameters){
        if(null == parameters){
            return;
        }
        mSupportedSize = parameters.getSupportedPreviewSizes();
        Camera.Size bestSize = getBestCameraSize(parameters.getSupportedPreviewSizes(), mExpectPreviewWidth, mExpectPreviewHeight);
        m_iCameraWidth = bestSize.width;
        m_iCameraHeight = bestSize.height;
        parameters.setPreviewSize(m_iCameraWidth, m_iCameraHeight);
        _log.i("majun_test:setPreviewSize %d*%d", m_iCameraWidth, m_iCameraHeight);
    }

    private void resetPreviewBuffers(){
        int framesize = m_iCameraWidth*m_iCameraHeight * 3 / 2;
        if(mPreviewBuffers == null || framesize != mPreviewBufferSize){
            mPreviewBuffers = new byte[mPreviewBufferCount][];
            mPreviewBufferSize = framesize;
            for(int i = 0 ; i < mPreviewBufferCount; ++i){
                mPreviewBuffers[i] = new byte[framesize];
            }
//            _log.i("stcrash_test : alloc preivewBuffers %d (%s,%s,%s,%s)",
//                    mPreviewBufferSize,
//                    mPreviewBuffers[0].hashCode(), mPreviewBuffers[1].hashCode(),
//                    mPreviewBuffers[2].hashCode(), mPreviewBuffers[3].hashCode()
//            );
        }
    }


    private Camera.Size getBestCameraSize(List<Camera.Size> supports, int expectWidth, int expectHeight){
        //从大到小排序
        Collections.sort(supports, m_compare);
        Camera.Size bestSize = supports.get(supports.size() - 1);

        for(Camera.Size size : supports){
            _log.i("majun_test : supportsize : " + size.width + "*" + size.height);
        }

        // 第一步 查找精确匹配的分辨率是否支持
        for(Camera.Size size : supports){
            if(expectWidth == size.width && expectHeight == size.height){
                bestSize = size;
                return bestSize;
            }
        }

        // 第二步 有些手机会32位对齐，判断是否支持
        int widthAlign = expectWidth % 32;
        int heightAlign = expectHeight % 32;
        int align32Width = widthAlign == 0 ? expectWidth : expectWidth + 32 - widthAlign;
        int align32Height = heightAlign == 0 ? expectHeight : expectHeight + 32 - heightAlign;

        for(Camera.Size size : supports){
            if(align32Width == size.width && align32Height == size.height){
                bestSize = size;
                return bestSize;
            }
        }

        if(msbLittleCameraResulotion){
            // 第三步 查找是否有9:16摄像头
            for(Camera.Size size : supports){
                if(size.width >= expectWidth){
                    continue;
                }

                if(size.width < CAMERA_PREVIEW_640){
                    break;
                }

                if(is9_16(size.width, size.height)){
                    bestSize = size;
                    return bestSize;
                }
            }
        }

        if(mFuncCode == CameraConst.FuncMode.K_ROOM){
            // 歌房默认640*480
            for(Camera.Size size : supports){
                if(CAMERA_PREVIEW_640 == size.width && CAMERA_PREVIEW_480 == size.height){
                    bestSize = size;
                    return bestSize;
                }
            }
        }

        // 第四步 查找支持1280*720的分辨率
        for(Camera.Size size : supports){
            if(CAMERA_PREVIEW_1280 == size.width && CAMERA_PREVIEW_720 == size.height){
                bestSize = size;
                return bestSize;
            }
        }

        return bestSize;
    }

    private int getBestCameraFps(List<Integer> supports, int expect){
        Collections.sort(supports);
        for (Integer fps : supports){
            _log.i("support fps : " + fps);
        }
        int bestFps = supports.get(supports.size() - 1);
        for(Integer fps : supports){
            if (expect == fps){
                bestFps = fps;
                return bestFps;
            }
        }

        for (Integer fps : supports){
            if(fps > expect){
                bestFps = fps;
                return bestFps;
            }
        }

        return bestFps;
    }

    private int getBestExplosure(int min, int max){
        int ret = min / 2 + 1;

        if (ret > max ){
            ret =  max;
        }
        _log.i("getBestExplosure : min = " + min + ", max = " + max + ", ret = " + ret);

        return (min + max) / 2 ;
    }

    private Camera getFrontCameraInstance(){
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private Camera getBackCameraInstance(){
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private Camera getDefaultCamera(int position){
        // Find the total number of cameras available
        int  mNumberOfCameras = Camera.getNumberOfCameras();

        m_iCameraNum = mNumberOfCameras;

        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == position) {
                return Camera.open(i);

            }
        }

        return null;
    }

    private boolean is9_16(int width, int height){
        return isEqual(1.0f * height / width, 1.0f * 9 / 16);
    }

    private boolean isEqual(float x, float y){
        return Math.abs(x - y) < FLOAT_PRECITION;
    }

    public void saveSP(){
        if (null != m_spCameraSet && (mbCameraSwitched || mbCameraPreivewSizeChanged)){
            SharedPreferences.Editor editor = m_spCameraSet.edit();
            if (null != editor){
                editor.clear();

                if(mbCameraSwitched){
                    editor.putBoolean(CameraConst.CameraSetting.PREV_CAMERA, m_bFrontCamera);
                }

                if(mbCameraPreivewSizeChanged){
                    if(mFuncCode == CameraConst.FuncMode.K_ROOM) {
                        editor.putInt(CameraConst.CameraSetting.PREV_WIDTH, mExpectPreviewWidth);
                        editor.putInt(CameraConst.CameraSetting.PREV_HEIGHT, mExpectPreviewHeight);
                    }
                }

                editor.commit();
            }
        }

    }

    public void release(){
        _log.i("release");
        saveSP();
        closeCamera();

        m_camera = null;
        mCameraInfo = null;
        mPreviewBuffers = null;

        m_spCameraSet = null;
        m_context = null;
        mbCameraPreivewSizeChanged = false;
        mbCameraSwitched = false;
    }
}
