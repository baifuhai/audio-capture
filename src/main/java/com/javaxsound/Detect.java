package com.javaxsound;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.List;

public class Detect {

	public static void main(String[] args) throws Exception {
//		Mixer.Info[] mixersInfo = AudioSystem.getMixerInfo();
//		for (Mixer.Info mixerInfo : mixersInfo) {
//			System.out.println("name: " + mixerInfo.getName());
//			System.out.println("vendor: " + mixerInfo.getVendor());
//			System.out.println("description: " + mixerInfo.getDescription());
//			System.out.println("version: " + mixerInfo.getVersion());
//			System.out.println();
//		}
//		System.out.println("-----------------------------------------");

		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, null);
		List<Line> lineList = MyAudioSystem.getLineList(dataLineInfo);
		System.out.println("match: " + lineList.size());
	}

}
