package com.javaxmedia;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Arrays.copyOfRange;

@Slf4j
public class WavUtil {

	private static final int HEAD_LENGTH = 12;

	private static final int FORMAT_LENGTH = 24;

	private static boolean isWav(byte[] head) {
		return ("RIFF".equals(new String(head, 0, 4, StandardCharsets.ISO_8859_1)) &&
				"WAVE".equals(new String(head, 8, 4, StandardCharsets.ISO_8859_1)));
	}

	private static void fileTooSmall(byte[] file) {
		if (file.length < HEAD_LENGTH + FORMAT_LENGTH) {
			log.warn("file is too small, size if {}.", file.length);
			throw new RuntimeException();
		}
	}

	private static int headSize() {
		return HEAD_LENGTH + FORMAT_LENGTH;
	}

	/**
	 * resolve wav file head.
	 * ChunkID,ChunkSize,Format, everyone 4 bytes.
	 */
	public static int fileSize(byte[] file) {
		fileTooSmall(file);

		byte[] head = copyOfRange(file, 0, HEAD_LENGTH);

		if (isWav(head)) {
			return ByteBuffer.wrap(copyOfRange(head, 4, 8))
					.order(ByteOrder.LITTLE_ENDIAN)
					.getInt() + 8;
		} else {
			log.warn("file format error: expected {}, actual {}.",
					"[82, 73, 70, 70, *, *, *, *, 87, 65, 86, 69]",
					head);
			throw new RuntimeException();
		}
	}

	public static AudioFormat fileFormat(byte[] file) {
		fileTooSmall(file);

		byte[] head = copyOfRange(file, 0, HEAD_LENGTH);

		if (isWav(head)) {
			byte[] format = copyOfRange(file, 12, HEAD_LENGTH + FORMAT_LENGTH);
			String chuckID = new String(format, 0, 4, StandardCharsets.ISO_8859_1);
			int chunkSize = ByteBuffer.wrap(copyOfRange(format, 4, 8))
					.order(ByteOrder.LITTLE_ENDIAN).getInt();
			int audioFmt = ByteBuffer.wrap(copyOfRange(format, 8, 10))
					.order(ByteOrder.LITTLE_ENDIAN).getShort();
			int channels = ByteBuffer.wrap(copyOfRange(format, 10, 12))
					.order(ByteOrder.LITTLE_ENDIAN).getShort();
			int sampleRate = ByteBuffer.wrap(copyOfRange(format, 12, 16))
					.order(ByteOrder.LITTLE_ENDIAN).getInt();
			int byteRate = ByteBuffer.wrap(copyOfRange(format, 16, 20))
					.order(ByteOrder.LITTLE_ENDIAN).getInt();
			int frameSize = ByteBuffer.wrap(copyOfRange(format, 20, 22))
					.order(ByteOrder.LITTLE_ENDIAN).getShort();
			int sampleSizeInBits = ByteBuffer.wrap(copyOfRange(format, 22, 24))
					.order(ByteOrder.LITTLE_ENDIAN).getShort();

			return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate,
					sampleSizeInBits, channels, frameSize, sampleRate, false);
		} else {
			log.warn("file is not a wav.");
			throw new RuntimeException();
		}
	}

	public static void merge(final byte[] left, final byte[] right, final String path) {
		int leftSize = fileSize(left);
		int rightSize = fileSize(right);
		int mergeSize = mergeSizeField(leftSize, rightSize);
		int mergeDataSize = mergeDataSize(leftSize, rightSize);

		try (RandomAccessFile file = new RandomAccessFile(path, "rw")) {
			file.write(mergeHead(left, mergeSize));
			file.write(dataChunkHead(mergeDataSize));
			int max = Math.max(leftSize, rightSize);
			for (int i = headSize() + 8; i < max + 8; i += 2) {
				file.write(read(left, i));
				file.write(read(right, i));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static byte[] read(final byte[] content, int offset) {
		if (content.length > offset) {
			return copyOfRange(content, offset, offset + 2);
		} else {
			return "\0\0".getBytes(StandardCharsets.ISO_8859_1);
		}
	}

	private static int mergeSizeField(int left, int right) {
		int max = Math.max(left - 8, right - 8);
		return max * 2;
	}

	private static int mergeDataSize(int left, int right) {
		int max = Math.max(left - headSize() - 8, right - headSize() - 8);
		return max * 2;
	}

	private static byte[] mergeHead(final byte[] left, final int mergeSize) {
		AudioFormat format = fileFormat(left);
		ByteBuffer size = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(mergeSize);

		ByteBuffer channels = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 2);
		ByteBuffer sampleRate = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) format.getSampleRate());
		ByteBuffer byteRate = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) format.getSampleRate() * 2 * format.getSampleSizeInBits() / 8);
		ByteBuffer blockAlign = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) (2 * format.getSampleSizeInBits() / 8));
		ByteBuffer bitsPerSample = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) format.getSampleSizeInBits());

		ByteBuffer head = ByteBuffer.allocate(headSize());
		head.put(left, 0, 4);

		head.put(size.array());
		head.put(left, 8, 14);

		head.put(channels.array());
		head.put(sampleRate.array());
		head.put(byteRate.array());
		head.put(blockAlign.array());
		head.put(bitsPerSample.array());
		return head.array();
	}

	private static byte[] dataChunkHead(final int length) {
		ByteBuffer head = ByteBuffer.allocate(8);
		head.put("data".getBytes(StandardCharsets.ISO_8859_1));
		ByteBuffer size = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(length);
		head.put(size.array());
		return head.array();
	}

	public static void main(String[] args) {
		try {
			byte[] left = Files.readAllBytes(Paths.get("d:/", "x.wav"));
			byte[] right = Files.readAllBytes(Paths.get("d:/", "y.wav"));
			merge(left, right, "d:/z.wav");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
