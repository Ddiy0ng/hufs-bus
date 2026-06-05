package hufsbus.spring.domain.favorite.service;

import hufsbus.spring.domain.favorite.dto.FavoriteCreateRequestDto;
import hufsbus.spring.domain.favorite.dto.FavoriteResponseDto;
import hufsbus.spring.domain.favorite.entity.Favorite;
import hufsbus.spring.domain.favorite.favoriteEnum.DayEnum;
import hufsbus.spring.domain.favorite.repository.FavoriteRepository;
import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.repository.BusStopRepository;
import hufsbus.spring.domain.timetable.repository.TimetableRepository;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import hufsbus.spring.domain.user.entity.User;
import hufsbus.spring.domain.user.repository.UserRepository;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final TimetableRepository timetableRepository;
    private final BusStopRepository busStopRepository;
    private final UserRepository userRepository;

    // 즐겨찾기 추가
    @Transactional
    public void createFavorite(FavoriteCreateRequestDto favoriteCreateRequestDto, Long userId) {

        Long timetableId = favoriteCreateRequestDto.getTimetableId();
        Set<DayEnum> days = favoriteCreateRequestDto.getDays();

        Timetable timetable = getTimetable(timetableId);
        User user = getUser(userId);

        for(DayEnum day : days) {
            boolean isExists = favoriteRepository.existsByUserAndTimetableAndDay(user, timetable, day);

            if(!isExists)
                favoriteRepository.save(Favorite.of(day, timetable, user));
        }
    }

    // 즐겨찾기 수정(currentLocation)
    @Transactional
    public void updateFavorite(FavoriteCreateRequestDto favoriteUpdateReuqestDto, Long userId) {

        Long timetableId = favoriteUpdateReuqestDto.getTimetableId();
        Set<DayEnum> requestDays = favoriteUpdateReuqestDto.getDays();

        Timetable timetable = getTimetable(timetableId);
        User user = getUser(userId);

        List<Favorite> currentFavorites = favoriteRepository.findAllByUserAndTimetable(user, timetable);

        // 기존에 존재하는 요일 set
        Set<DayEnum> currentDays = currentFavorites.stream()
                .map(Favorite::getDay)
                .collect(Collectors.toSet());

        // 요청에는 없는 요일 삭제
        for (Favorite favorite : currentFavorites) {
            if (!requestDays.contains(favorite.getDay())) {
                favoriteRepository.delete(favorite);
            }
        }

        // 요청에는 있는 요일 추가
        for (DayEnum requestDay : requestDays) {
            if (!currentDays.contains(requestDay)) {
                favoriteRepository.save(Favorite.of(requestDay, timetable, user));
            }
        }
    }

    // 즐겨찾기 삭제(즐겨찾기 탭)
    @Transactional
    public void removeFavorite(Long favoriteId, Long userId, DayEnum day) {

        User user = getUser(userId);

        favoriteRepository.deleteByIdAndUserAndDay(favoriteId, user, day);
    }

    // 즐겨찾기 탭에서 즐겨찾기 조회(default = 교내/월, requestParam = 교내외/요일)
    @Transactional
    public List<FavoriteResponseDto> searchFavorites(InOutCampusEnum inOutCampusEnum, Long userId, DayEnum day) {

        User user = getUser(userId);
        List<Favorite> favoriteList = favoriteRepository.searchFavorites(user, day, inOutCampusEnum);

        return favoriteList.stream()
                .map(favorite -> {
                    BusRoute busRoute = favorite.getTimetable().getBusRoute();

                    List<BusStop> busStopList =
                            busStopRepository.findByBusRouteOrderByStopOrderAsc(busRoute);

                    return FavoriteResponseDto.of(favorite, busStopList);
                })
                .toList();
    }

    // 즐겨찾기 여부 확인(currentLocation에서 즐겨찾기 여부 확인)
    public boolean isFavorite (Long timetableId, Long userId) {

        Timetable timetable = getTimetable(timetableId);
        User user = getUser(userId);

        List<Favorite> favorites = favoriteRepository.findAllByUserAndTimetable(user, timetable);

        boolean isFavorite;
        if(favorites.isEmpty())
            isFavorite = true;
        else
            isFavorite = false;

        return isFavorite;
    }

    private Timetable getTimetable(Long timetableId) {

        Optional<Timetable> optionalTimetable = timetableRepository.findById(timetableId);
        if(optionalTimetable.isEmpty())
            throw new CustomException(ErrorCode.TIMETABLE_NOT_EXIST_EXCEPTION);
        Timetable timetable = optionalTimetable.get();

        return timetable;
    }

    private User getUser(Long userId) {

        Optional<User> optionalUser = userRepository.findById(userId);
        if(optionalUser.isEmpty())
            throw new CustomException(ErrorCode.USER_NOT_EXIST_EXCEPTION);
        User user = optionalUser.get();

        return user;
    }


}
