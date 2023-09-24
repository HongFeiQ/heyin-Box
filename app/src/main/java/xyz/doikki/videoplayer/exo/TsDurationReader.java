package xyz.doikki.videoplayer.exo;

import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.PositionHolder;

public class TsDurationReader {
    public TsDurationReader(int timestampSearchBytes) {
    }

    public int readDuration(ExtractorInput input, PositionHolder seekPosition, int pcrPid) {
        return pcrPid;
    }

    public boolean isDurationReadFinished() {
        return false;
    }

    public long getDurationUs() {
        return 0;
    }

    public Object getPcrTimestampAdjuster() {
        return null;
    }
}
