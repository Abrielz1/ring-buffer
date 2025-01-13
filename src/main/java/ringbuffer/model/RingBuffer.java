package main.java.ringbuffer.model;

import lombok.Getter;
import lombok.ToString;

@ToString
public class RingBuffer {

    private final Object lock = new Object();

    private final byte[] buffer;

    private int index;

    private int size;

    private boolean isWriting;

    private boolean isReading;

    public RingBuffer(int size) {

        if (size <= 0) {

            throw new IllegalArgumentException("Size must be greater than 0");
        }

        buffer = new byte[size];
    }

    public int getIndex() {

        synchronized (lock) {
            return index;
        }
    }

    public int size() {

        return buffer.length;
    }

    public byte[] getBuffer() {

        return buffer;
    }


    public boolean isEmpty() {

        return buffer.length == 0;
    }

    public boolean isFull() {

        return size == buffer.length;
    }

    public void clear() {

        if (!isReading && !isWriting) {

            size = 0;

            index = 0;
        } else {

            throw new IllegalStateException("Cannot clear buffer when someone is accessing");
        }
    }

    public static void check(final int maxLength, final Range range) {

        if (maxLength < 0 || range == null) {
            throw new IllegalArgumentException("Length mast be grater than 0!");
        }
    }

    private static int min(int first, int second) {

        return Math.min(first, second);
    }

    private static int min(int first, int second, int third) {

        return Math.min(Math.min(first, second), third);
    }

    public void writingBegin(final int maxLength, final Range range) {

        synchronized (lock) {

            if (isWriting) {

                throw new IllegalStateException("Cannot begin writing until previous finished");
            }

            isWriting = true;

            check(maxLength, range);

            if (size != buffer.length && maxLength > 0) {

                range.start = (index + size) % buffer.length;

                if (range.start < index) {

                    range.end = min(range.start, +maxLength, index) - 1;

                } else {

                    range.end = min(range.start + maxLength, buffer.length) - 1;
                }

            } else {

                range.start = range.end = Range.INVALID_INDEX;
            }
        }
    }

    public void writingEnd(final int written) {

        synchronized (lock) {
            try {
                if (!isWriting) {

                    throw new IllegalStateException("Cannot stop what is not begin!");
                }

                if (written < 0) {
                    throw new IllegalArgumentException("Cannot write data less when 0");
                }

                if (size + written > buffer.length) {
                    throw new IllegalArgumentException("Written size overflow");
                }

                size += written;

            } finally {

                isWriting = false;
            }
        }
    }

    public void readingBegin(final int maxLength, final Range range) {

        synchronized (lock) {

            if (isReading) {
                throw new IllegalStateException("You must finish other attempt!");
            }

            isReading = true;

            check(maxLength, range);
        }

        if (size > 0 && maxLength > 0) {

            range.start = index;
            range.end = min(index + maxLength, index + size, buffer.length) - 1;

        } else {

            range.start = range.end = Range.INVALID_INDEX;
        }
    }

    public void readingEnd(final int readed) {

        synchronized (lock) {
            try {

                if (!isReading) {
                    throw new IllegalStateException("Cannot finish what is not started yet!");
                }

                if (readed < 0) {
                    throw new IllegalArgumentException("Read less than 0");
                }

                if (readed > size) {
                    throw new IllegalArgumentException("Read size overflow");
                }

                size -= readed;
                index = (index + readed) % buffer.length;

            } finally {

                isReading = false;
            }
        }
    }


    @Getter
    @ToString
    public static class Range {

        public final static int INVALID_INDEX = -1;

        private int start = INVALID_INDEX;

        private int end = INVALID_INDEX;

        public boolean isValid() {

            return start != INVALID_INDEX && end != INVALID_INDEX;
        }

        public int getLength() {

            return isValid() ? end - start + 1 : 0;
        }

    }
}
