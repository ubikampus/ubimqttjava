package fi.helsinki.ubimqtt;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ReplayDetector {

    private SortedMap<Long, Map<String, Boolean>> buffer;
    private int maxBufferSize = -1;

    public ReplayDetector(int maxBufferSize) {

        this.buffer = new TreeMap<Long, Map<String, Boolean>>();
        this.maxBufferSize = maxBufferSize;
        addEntry(System.currentTimeMillis(), "");
    }

    private void addEntry(long timestamp, String nonce) {

        Map<String, Boolean> messages = null;

        if (!buffer.containsKey(timestamp)) {
            messages = new HashMap<String, Boolean>();
            buffer.put(timestamp, messages);
        } else {
            messages = buffer.get(timestamp);
        }

        messages.put(nonce, true);
    }

    public boolean isValid(long timestamp, String nonce) {

        // Reject messages that are older than the oldest entry in the buffer
        if (buffer.size() != 0 && timestamp < buffer.firstKey())
            return false;

        // Reject message If there is an entry with exactly same timestamp and nonce
        if (buffer.containsKey(timestamp) && buffer.get(timestamp).containsKey(nonce))
            return false;

        // If buffer is growing too large, remove an entry
        if (buffer.size() == maxBufferSize)
            buffer.remove(buffer.firstKey());

        // Message is accptable, add it to the buffer

        addEntry(timestamp, nonce);

        return true;
    }

}
