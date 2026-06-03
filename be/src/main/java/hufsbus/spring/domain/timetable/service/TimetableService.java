package hufsbus.spring.domain.timetable.service;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.timetable.dto.DepartResponse;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.repository.TimetableRepository;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final BusRepository busRepository;

    @Transactional
    public DepartResponse depart(Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMETABLE_NOT_FOUND));

        Bus bus = busRepository.findByTimetableId(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        timetable.depart(LocalTime.now());
        bus.startRunning();

        return DepartResponse.of(timetable, bus);
    }
}
