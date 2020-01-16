//package com.zdpower.voice.mt_scylla;
//import com.google.gson.Gson;
//import com.iflytek.mt_scylla.mt_scylla;
//import com.zdpower.voice.repository.VoiceRepository;
//import com.zdpower.voice.service.VoiceService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.sound.sampled.*;
//import java.io.*;
//
///**
// * 服务器自启类  捕获音频生成文件
// */
//@Slf4j
//@Component
//public class EngineCoreNew {
//
//    @Value("${path.prefix}")
//    private String pathPrefix;
//    @Value("${path.suffix}")
//    private String pathSuffix;
//    @Value("${xfyun.inputIp}")
//    private String inputIp;
//    @Value("${xfyun.inputRecord}")
//    private String inputRecord;
//    @Value("${xfyun.count}")
//    private Integer count;
//    @Value("${xfyun.weight}")
//    private Integer weight;
//    @Value("${cfg}")
//    private String cfg;
//    private static String allResult;
//    private static String ssbparam;
//    private static mt_scylla mt;
//    private static String session_id;
//    private static Gson gson = new Gson();
//    private static Attribute product = new Attribute();
//    private static boolean b_online = true;
//    private static String sId = "";
//    private static Boolean toSpeex = false;
//    private static String aueParam = "";
//
//    private AudioFormat audioFormat;
//
//    private TargetDataLine targetDataLine;
//
//    private boolean flag = true;
//
//    @Autowired
//    private VoiceRepository voiceRepository;
//    @Autowired
//    private VoiceService voiceService;
//
//
//    /**
//     * 识别音频
//     */
//    public void startRecognize(){
//        // 获得指定的音频格式
//        audioFormat = getAudioFormat();
//        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
//        try {
//            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
//        } catch (LineUnavailableException e) {
//            e.printStackTrace();
//        }
//        flag = true;
//        // 创建一个线程来捕获麦克风
//        //将数据输入音频文件并启动
//        //线程正在运行。它会一直运行到
//        //点击停止按钮。这种方法
//        //启动线程后返回。
//        new CaptureThread().start();
//    }
//
//    class CaptureThread extends Thread {
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    getParam(cfg);
//                    allResult = "";
//                    mt = new mt_scylla();
//                    // 初始化语音识别引擎
//                    int initret = mt.SCYMTInitializeEx(null);
//                    String parL = "appid=pc20onli,sn=c" + ",url=" + inputIp;
//                    mt.SCYMTAuthLogin(parL, null);
//                    if (initret != 0) {
//                        log.error("请检查IP地址是否正确、网络是否正常开启,错误码是" + initret);
//                        return;
//                    }
//                    if (b_online) {
//                        ssbparam = "svc=iat,auf=audio/L16;rate=16000,aue=speex-wb,type=1,uid=5577,appid=pc20onli,hotword=hotword.txt,url=" + inputIp;
//                    }
//
//                        int[] errorCode = new int[1];
//                        session_id = mt.SCYMTSessionBeginEx(ssbparam, errorCode, null);
//                        if (errorCode[0] != 0) {
//                            String error = "请检查IP地址是否正确、网络是否正常开启,错误码是" + errorCode[0];
//                            log.error(error);
//                            return;
//                        }
//                        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000F, 16, 1, 2, 16000F, false);
//                        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
//                        TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
//                        targetDataLine.open(audioFormat);
//
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        //判断是否停止的计数
//                        int downSum = 20;
//                        //麦克风启动
//                        targetDataLine.start();
//                        final int bufSize = 1024;
//                        byte[] buffer = new byte[bufSize];
//                        int[] epStatus = new int[1];
//                        int[] recogStatus = new int[1];
//                        int[] ret = new int[1];
//                        int audioStatus = 1;
//                        while (flag) {
//                            //获取麦克风音频
//                            targetDataLine.read(buffer, 0, bufSize);
//                            //当数组末位大于weight时开始存储字节（有声音传入），一旦开始不再需要判断末位
//                            if (Math.abs(buffer[buffer.length - 1]) > weight || baos.size() > 0) {
//                                baos.write(1);
//                                String cur_res = mt.SCYMTAudioWriteEx(session_id, buffer, buffer.length, audioStatus, epStatus, recogStatus, ret, null);
//                                log.info("ret[0]:{}", ret[0]);
//                                if (ret[0] != 0) {
//                                    String err = "上传音频出错，错误码为：" + ret[0];
//                                    log.error(err);
//                                    flag = false;
//                                }
//                                audioStatus = 2;
//                                // 判断引擎返回pgs是否为1，为1表示有识别结果可获取
//                                if (ret[0] == 0 && cur_res.length() != 0) {
//                                    //解析json
//                                    product = gson.fromJson(cur_res, Attribute.class);
//                                    //输出
//                                    if (product.getPgs() == 1) {
//                                        //获取识别结果
//                                        allResult = allResult + product.getResult();
//                                        log.info("record_iat:{}", cur_res);
//                                        log.info("allResult:{}", allResult);
//                                        flag = false;
//                                    }
//                                }
//                                log.info("Math.abs(buffer[buffer.length - 1]:{}", Math.abs(buffer[buffer.length - 1]));
//                                //判断语音是否停止
//                                if (Math.abs(buffer[buffer.length - 1]) <= weight) {
//                                    downSum++;
//                                } else {
//                                    System.out.println("重置奇数");
//                                    downSum = 0;
//                                    audioStatus = 2;
//                                }
//                                //计数超过20说明此段时间没有声音传入(值也可更改)
//
//                                if (downSum > 20 && audioStatus == 4) {
//                                    log.info("停止录入");
//                                    flag = false;
//                                }
//                                if (downSum > 20) {
//                                    audioStatus = 4;
//                                }
//                            }
//                        }
//                        //麦克风关闭
//                        targetDataLine.stop();
//                        targetDataLine.close();
//                        flag = true;
//                        baos.reset();
//                        // 结束一路会话
//                        int endret = mt.SCYMTSessionEndEx(session_id);
//                        if (endret != 0) {
//                            String error = "会话关闭失败,错误码是" + endret;
//                            log.error(error);
////                            return;
//                        }
//                    // 逆初始化
//                    int uniret = mt.SCYMTUninitializeEx(null);
//                    if (uniret != 0) {
//                        String error = "逆初始化失败,错误码是" + uniret;
//                        log.error(error);
////                        return;
//                    }
//                    log.info(allResult);
////                    return;
//                } catch (Exception e) {
//                    e.printStackTrace();
////                    return;
//                }
//            }
//        }
//    }
//
//    /**
//     * 结束捕获音频方法
//     */
//    private void stopRecognize() {
//        flag = false;
//        targetDataLine.stop();
//        targetDataLine.close();
//    }
//
//    /**
//     * 音频格式
//     *
//     * @return
//     */
//    private AudioFormat getAudioFormat() {
//        float sampleRate = 16000;
//        // 8000,11025,16000,22050,44100
//        int sampleSizeInBits = 16;
//        // 8,16
//        int channels = 1;
//        // 1,2
//        boolean signed = true;
//        // true,false
//        boolean bigEndian = false;
//        // true,false
//        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
//    }// 结束获取音频格式
//
//
//    public static void record_iat() {
//
//
//    }
//
//
//    private static void getParam(String fileName) throws IOException {
//        FileInputStream inputStream = new FileInputStream(fileName);
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//        String str;
//        while ((str = bufferedReader.readLine()) != null) {
//            String[] param = str.split("=");
//
//            if (param != null && !param.equals("") && param.length >= 2) {
//                if (param[0].equals("speex_aue")) {
//                    if (param[1].equals("speex") || param[1].equals("speex-wb")) {
//                        aueParam = param[1];
//                        toSpeex = true;
//                    } else {
//                        aueParam = "raw";
//                        toSpeex = false;
//                    }
//                }
//            }
//        }
//
//        bufferedReader.close();
//        inputStream.close();
//    }
//
//}
