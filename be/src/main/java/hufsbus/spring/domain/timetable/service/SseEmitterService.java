package hufsbus.spring.domain.timetable.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEmitterService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long timetableId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(timetableId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(timetableId, emitter));
        emitter.onTimeout(() -> remove(timetableId, emitter));
        emitter.onError(e -> remove(timetableId, emitter));

        return emitter;
    }

    public void broadcast(Long timetableId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(timetableId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    private void remove(Long timetableId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(timetableId);
        if (list != null) list.remove(emitter);
    }
}
