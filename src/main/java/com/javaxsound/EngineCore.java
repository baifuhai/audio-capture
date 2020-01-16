package com.javaxsound;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class EngineCore {

	private boolean flag = true;

	public static void main(String args[]) throws Exception {
		EngineCore engineCore = new EngineCore();
		engineCore.startRecognize();
	}

	public void startRecognize() throws Exception {
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, null);

		// checks if system supports the data line
		if (!AudioSystem.isLineSupported(dataLineInfo)) {
			throw new RuntimeException("line not supported for format");
		}

		List<Line> lineList = MyAudioSystem.getLineList(dataLineInfo);
		for (int i = 0; i < lineList.size(); i++) {
			new CaptureThread(i + 1, (TargetDataLine) lineList.get(i)).start();
		}
	}

	class CaptureThread extends Thread {
		private int i;
		private TargetDataLine targetDataLine;

		public CaptureThread(int i, TargetDataLine targetDataLine) {
			this.i = i;
			this.targetDataLine = targetDataLine;
		}

		@Override
		public void run() {
			//声音录入的权值
			int weight = 2;

			//判断是否停止的计数
			int count = 0;
			int maxCount = 10;

			ByteArrayInputStream bais = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			AudioInputStream ais = null;
			try {
				// 获得指定的音频格式
				AudioFormat audioFormat = getAudioFormat();

				targetDataLine.open(audioFormat);
				targetDataLine.start();

				// 获得当前音频采样率
				int sampleRate = (int) audioFormat.getSampleRate();
				// 获取当前音频通道数量
				int numChannels = audioFormat.getChannels();
				// 初始化音频缓冲区(size是音频采样率*通道数)
				int audioBufferSize = sampleRate * numChannels;

				byte[] buf = new byte[1024];

				while (flag) {
					targetDataLine.read(buf, 0, buf.length);
					//当数组末位大于weight时开始存储字节（有声音传入），一旦开始不再需要判断末位
					if (Math.abs(buf[buf.length - 1]) > weight || baos.size() > 0) {
						baos.write(buf);
						System.out.println("首位: " + buf[0] + ", 末位: " + buf[buf.length - 1] + ", length: " + buf.length);

						//判断语音是否停止
						if (Math.abs(buf[buf.length - 1]) <= weight) {
							System.out.println("计数+1");
							count++;
						} else {
							System.out.println("重置计数");
							count = 0;
						}

						//计数超过maxCount说明此段时间没有声音传入
						if (count > maxCount) {
							System.out.println("停止录入");
							break;
						}
					}
				}

				//生成语音文件
				System.out.println("开始生成语音文件");
				byte[] audioData = baos.toByteArray();
				bais = new ByteArrayInputStream(audioData);
				ais = new AudioInputStream(bais, audioFormat, audioData.length / audioFormat.getFrameSize());
				AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
				File audioFile = new File("d:/sound" + i + ".wav");
				AudioSystem.write(ais, fileType, audioFile);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (ais != null) {
					try {
						ais.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (bais != null) {
					try {
						bais.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (targetDataLine != null) {
					targetDataLine.stop();
					targetDataLine.close();
				}
			}
		}
	}

	public void stopRecognize() {
		flag = false;
	}

	private AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		// 8000,11025,16000,22050,44100
		int sampleSizeInBits = 16;
		// 8,16
		int channels = 1;
		// 1,2
		boolean signed = true;
		// true,false
		boolean bigEndian = false;
		// true,false
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

}