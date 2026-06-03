package hufsbus.spring.domain.favorite.repository;

import hufsbus.spring.domain.auth.entity.User;
import hufsbus.spring.domain.favorite.entity.Favorite;
import hufsbus.spring.domain.favorite.favoriteEnum.DayEnum;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite,Long> {

    boolean existsByUserAndTimetableAndDay(User user, Timetable timetable, DayEnum day);

    List<Favorite> findAllByUserAndTimetable(User user, Timetable timetable);

    @Query("""
            select f
            from Favorite f
            join fetch f.timetable t
            join fetch t.busRoute br
            where f.user = :user
              and f.day = :day
              and br.inOutCampus = :inOutCampus
            order by t.departAt asc
            """)
    List<Favorite> searchFavorites(
            @Param("user") User user,
            @Param("day") DayEnum day,
            @Param("inOutCampus") InOutCampusEnum inOutCampus
    );

    List<Favorite> findAllByUserAndDay(User user, DayEnum day);

    void deleteByIdAndUserAndDay(Long id, User user, DayEnum day);
}
