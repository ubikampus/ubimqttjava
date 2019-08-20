package fi.helsinki.ubimqtt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ReplayDetector {

    private SortedMap<Long, Map<String, Boolean>> buffer;
    private int bufferWindowInSeconds = -1;

    public ReplayDetector(int bufferWindowInSeconds) {

        this.buffer = new TreeMap<>();
        this.bufferWindowInSeconds = bufferWindowInSeconds;
        //addEntry(System.currentTimeMillis(), "");
    }

    private void addEntry(long timestamp, String messageId) {

        Map<String, Boolean> messages;

        if (!buffer.containsKey(timestamp)) {
            messages = new HashMap<>();
            buffer.put(timestamp, messages);
        } else {
            messages = buffer.get(timestamp);
        }

        messages.put(messageId, true);
    }

    public boolean isValid(long timestamp, String messageId) {
        // Reject messages that are older than the bufferWindowInSeconds

        if (timestamp< System.currentTimeMillis() - (bufferWindowInSeconds*1000))
            return false;

        // Reject message If there is an entry with exactly same timestamp and messageId
        if (buffer.containsKey(timestamp) && buffer.get(timestamp).containsKey(messageId))
            return false;

        // Remove entries that are older than bufferWindowInSeconds from buffer

        Iterator<Long> iterator = buffer.keySet().iterator();

        while (iterator.hasNext()) {
            long key = iterator.next();
            if (key < System.currentTimeMillis() - (bufferWindowInSeconds*1000))
                iterator.remove();
            else
                break;
        }
        // Message is accptable, add it to the buffer

        addEntry(timestamp, messageId);

        return true;
    }

}
