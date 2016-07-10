#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <string>
#include <vector>
#include <sstream>
#include "fstream"
#include "util.h"
#include "easypr.h"
#include "easypr/util/switch.hpp"
using namespace std;
using namespace cv;
using namespace easypr;
#ifdef __cplusplus
extern "C" {
#endif
string mappingpath="/sdcard/mrcar/chinese_mapping";
JNIEXPORT jstring JNICALL
Java_yanyu_com_mrcar_MainActivity_plateRecognition(JNIEnv *env, jclass type, jlong matImg,
                                                   jlong matResult) {

    // TODO
    Mat &img=*(Mat *)matImg;
    cvtColor(img, img, cv::COLOR_RGBA2BGR);
    Mat &result=*(Mat *)matResult;
    vector<Mat> resultVec;
    CPlateRecognize pr;
    pr.setResultShow(false);
    pr.setDetectType(PR_DETECT_CMSER);
    string license;
    vector<CPlate> plateVec;
    int re= pr.plateRecognize(img, plateVec);
    for (int i = 0; i < plateVec.size(); i++)
    {
        CPlate plate = plateVec.at(i);
        license = plate.getPlateStr();
        break;
    }
    result=img.clone();
    putText(img,license,Point(500,500),CV_FONT_HERSHEY_COMPLEX,1.0f,Scalar(255, 0, 0));
    cvtColor(result,result, cv::COLOR_RGB2BGRA);
    LOGI("end plateRecognition");
    return env->NewStringUTF(license.c_str());
}

#ifdef __cplusplus
}
#endif