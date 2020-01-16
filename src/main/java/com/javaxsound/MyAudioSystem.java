package com.javaxsound;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.spi.MixerProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MyAudioSystem {

	private static List getMixerProviders() throws Exception {
		Method method = AudioSystem.class.getDeclaredMethod("getMixerProviders");
		method.setAccessible(true);
		return (List) method.invoke(null);
	}

	private static boolean isAppropriateMixer(Mixer mixer, Line.Info lineInfo, boolean isMixingRequired) throws Exception {
		Method method = AudioSystem.class.getDeclaredMethod("isAppropriateMixer", Mixer.class, Line.Info.class, boolean.class);
		method.setAccessible(true);
		return (boolean) method.invoke(null, mixer, lineInfo, isMixingRequired);
	}

	public static List<Line> getLineList(Line.Info lineInfo) throws Exception {
		List<Line> lineList = new ArrayList<>();

		LineUnavailableException lue = null;

		List providers = getMixerProviders();

		for (int i = 0; i < providers.size(); i++) {
			MixerProvider provider = (MixerProvider) providers.get(i);
			Mixer.Info[] infos = provider.getMixerInfo();
			for (int j = 0; j < infos.length; j++) {
				Mixer.Info mixerInfo = infos[j];
				System.out.println("name: " + mixerInfo.getName());
				System.out.println("vendor: " + mixerInfo.getVendor());
				System.out.println("description: " + mixerInfo.getDescription());
				System.out.println("version: " + mixerInfo.getVersion());
				try {
					Mixer mixer = provider.getMixer(mixerInfo);
					if (isAppropriateMixer(mixer, lineInfo, true)
							|| isAppropriateMixer(mixer, lineInfo, false)) {
						System.out.println("【match】");
						lineList.add(mixer.getLine(lineInfo));
					}
				} catch (LineUnavailableException e) {
					lue = e;
				} catch (IllegalArgumentException iae) {
					// must not happen... but better to catch it here,
					// if plug-ins are badly written
				}
				System.out.println();
			}
		}

		if (lue != null) {
			throw lue;
		}

		if (lineList.size() >= 2) {
			lineList.remove(0);
		}

		return lineList;
	}

}
