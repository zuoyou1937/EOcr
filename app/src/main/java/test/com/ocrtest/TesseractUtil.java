package test.com.ocrtest;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;

/**
 * Created by Sikang on 2017/7/12.
 */

public class TesseractUtil {
    //字体库路径，必须包含tessdata文件夹
    static final String TESSBASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/tesseract";
    //识别语言英文
    static final String NUMBER_LANGUAGE = "eng";

    private static TesseractUtil mTesseractUtil = null;

    private TesseractUtil() {

    }

    public static TesseractUtil getInstance() {
        if (mTesseractUtil == null)
            synchronized (TesseractUtil.class) {
                if (mTesseractUtil == null)
                    mTesseractUtil = new TesseractUtil();
            }
        return mTesseractUtil;
    }


    /**
     * 识别字符
     *
     * @param bmp      需要识别的图片
     * @param callBack 结果回调（携带一个String 参数即可）
     */

    public void scanOcr(final Bitmap bmp, final SimpleCallback callBack) {
        if (checkFontData()) {
            TessBaseAPI baseApi = new TessBaseAPI();
            //初始化OCR的字体数据，TESSBASE_PATH为路径，ENGLISH_LANGUAGE指明要用的字体库（不用加后缀）
            if (baseApi.init(TESSBASE_PATH, NUMBER_LANGUAGE)) {
                //设置识别模式
                //baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
                //设置要识别的图片
                baseApi.setImage(bmp);
                //baseApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);
                //开始识别
                String result = baseApi.getUTF8Text();
                baseApi.clear();
                baseApi.end();
                bmp.recycle();
                callBack.response(result);
            }
        }
    }

    /**
     * 检查语言包
     */
    public boolean checkFontData() {
        File file = new File(TESSBASE_PATH + "/tessdata/");
        if (!file.exists())
            throw new RuntimeException("没有找到正确的字库目录 : \"" + TESSBASE_PATH + "/tessdata/\"");


        String fontPath = TESSBASE_PATH + "/tessdata/eng.traineddata";
        file = new File(fontPath);
        if (!file.exists())
            throw new RuntimeException("没有找到正确的字库目文件 : \"" + TESSBASE_PATH + "/tessdata/eng.traineddata\"");

        return true;
    }


    /**
     * 清除干扰
     */
    /*private boolean clearInterfere(int[] pixels, int[] used, int[] charRect, int width, int height, int maxWidth, int maxHeight, int x, int y) {
        int nowX = x;
        int nowY = y;
        //记录动作
        Stack<Integer> stepStack = new Stack<>();
        boolean needReset = true;
        while (true) {
            if (used[width * nowY + nowX] == WAIT_HANDLE) {
                used[width * nowY + nowX] = HANDLED;

                if (charRect[2] - charRect[0] <= maxWidth && charRect[3] - charRect[1] <= maxHeight) {
                    if (charRect[0] > nowX)
                        charRect[0] = nowX;

                    if (charRect[1] > nowY)
                        charRect[1] = nowY;

                    if (charRect[2] < nowX)
                        charRect[2] = nowX;

                    if (charRect[3] < nowY)
                        charRect[3] = nowY;
                } else {
                    if (needReset)
                        needReset = false;
                    used[width * nowY + nowX] = HANDLING;
                    pixels[width * nowY + nowX] = PX_UNKNOW;
                }
            } else if (pixels[width * nowY + nowX] == PX_UNKNOW) {

                if (charRect[2] - charRect[0] <= maxWidth && charRect[3] - charRect[1] <= maxHeight) {
                    pixels[width * nowY + nowX] = PX_BLACK;
                    if (charRect[0] > nowX)
                        charRect[0] = nowX;

                    if (charRect[1] > nowY)
                        charRect[1] = nowY;

                    if (charRect[2] < nowX)
                        charRect[2] = nowX;

                    if (charRect[3] < nowY)
                        charRect[3] = nowY;

                    used[width * nowY + nowX] = HANDLED;
                } else {
                    if (needReset)
                        needReset = false;
                }
            }


            //当前像素的左边是否还有黑色像素点
            int leftX = nowX - 1;
            int leftIndex = width * nowY + leftX;
            if (leftX >= 0 && pixels[leftIndex] != PX_WHITE && (used[leftIndex] == WAIT_HANDLE || (needReset && used[leftIndex] == HANDLING))) {
                nowX = leftX;
                stepStack.push(MOVE_LEFT);
                continue;
            }

            //当前像素的上边是否还有黑色像素点
            int topY = nowY - 1;
            int topIndex = width * topY + nowX;
            if (topY >= 0 && pixels[topIndex] != PX_WHITE && (used[topIndex] == WAIT_HANDLE || (needReset && used[topIndex] == HANDLING))) {
                nowY = topY;
                stepStack.push(MOVE_TOP);
                continue;
            }


            //当前像素的右边是否还有黑色像素点
            int rightX = nowX + 1;
            int rightIndex = width * nowY + rightX;
            if (rightX < width && pixels[rightIndex] != PX_WHITE && (used[rightIndex] == WAIT_HANDLE || (needReset && used[rightIndex] == HANDLING))) {
                nowX = rightX;
                stepStack.push(MOVE_RIGHT);
                continue;
            }


            //当前像素的下边是否还有黑色像素点
            int bottomY = nowY + 1;
            int bottomIndex = width * bottomY + nowX;
            if (bottomY < height && pixels[bottomIndex] != PX_WHITE && (used[bottomIndex] == WAIT_HANDLE || (needReset && used[bottomIndex] == HANDLING))) {
                nowY = bottomY;
                stepStack.push(MOVE_BOTTOM);
                continue;
            }

            if (stepStack.size() > 0) {
                int step = stepStack.pop();
                switch (step) {
                    case MOVE_LEFT:
                        nowX++;
                        break;
                    case MOVE_RIGHT:
                        nowX--;
                        break;
                    case MOVE_TOP:
                        nowY++;
                        break;
                    case MOVE_BOTTOM:
                        nowY--;
                        break;
                }
            } else {
                break;
            }
        }
        return true;
    }*/

}
