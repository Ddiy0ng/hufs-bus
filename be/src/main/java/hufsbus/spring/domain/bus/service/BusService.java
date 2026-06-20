package hufsbus.spring.domain.bus.service;

import hufsbus.spring.domain.bus.dto.BusResponse;
import hufsbus.spring.domain.bus.dto.BusTagRequest;
import hufsbus.spring.domain.bus.dto.BusTagResponse;
import hufsbus.spring.domain.bus.dto.SeatUpdateEvent;
import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.timetable.service.SseEmitterService;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusService {
    private final BusRepository busRepository;
    private final SseEmitterService sseEmitterService;

    public BusResponse getSeats(Long timetableId) {
        Bus bus = busRepository.findByTimetableId(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));
        return BusResponse.of(bus);
    }

    @Transactional
    public BusTagResponse tag(Long timetableId, BusTagRequest request) {
        Bus bus = busRepository.findByTimetableId(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        if (request.getType() == BusTagRequest.TagType.BOARD) {
            if (bus.getCurrentSeats() >= bus.getTotalSeats()) {
                throw new CustomException(ErrorCode.SEAT_UNAVAILABLE);
            }
            bus.board();
        } else {
            if (bus.getCurrentSeats() <= 0) {
                throw new CustomException(ErrorCode.NO_PASSENGER);
            }
            bus.alight();
        }

        sseEmitterService.broadcast(timetableId, "seat-update", SeatUpdateEvent.of(timetableId, bus, request.getType()));

        return BusTagResponse.of(bus, request.getType());
    }
}
