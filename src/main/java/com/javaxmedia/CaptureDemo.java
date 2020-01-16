package com.javaxmedia;

import jmapps.util.StateHelper;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.DataSink;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.control.StreamWriterControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import java.util.Vector;

public class CaptureDemo {

	public static void main(String[] args) throws Exception {
		Vector deviceList = CaptureDeviceManager.getDeviceList(new AudioFormat(null));
		if (deviceList.size() == 0) {
			System.out.println("no audio capture device found");
			return;
		}

		CaptureDeviceInfo captureDeviceInfo = (CaptureDeviceInfo) deviceList.elementAt(0);

		Processor processor = Manager.createProcessor(captureDeviceInfo.getLocator());

		StateHelper stateHelper = new StateHelper(processor);

		if (!stateHelper.configure(10000)) {
			System.exit(-1);
		}

		processor.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));

		if (!stateHelper.realize(10000)) {
			System.exit(-1);
		}

		DataSource source = processor.getDataOutput();
		MediaLocator dest = new MediaLocator("file:///d:/foo.wav");
		DataSink dataSink = Manager.createDataSink(source, dest);

		dataSink.open();

		StreamWriterControl swc = (StreamWriterControl) processor.getControl("javax.media.control.StreamWriterControl");
		if (swc != null) {
			swc.setStreamSizeLimit(500000000);
		}

		System.out.println("start");

		dataSink.start();
		stateHelper.playToEndOfMedia(5 * 1000);

		stateHelper.close();
		dataSink.close();

		System.out.println("end");
	}

}


