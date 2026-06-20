package hufsbus.spring.domain.timetable.dto;

import lombok.Getter;

@Getter
public class TimetableUploadResponse {
    private final int createdTimetableCount;
    private final int createdBusCount;

    private TimetableUploadResponse(int createdTimetableCount, int createdBusCount) {
        this.createdTimetableCount = createdTimetableCount;
        this.createdBusCount = createdBusCount;
    }

    public static TimetableUploadResponse of(int createdTimetableCount, int createdBusCount) {
        return new TimetableUploadResponse(createdTimetableCount, createdBusCount);
    }
}
