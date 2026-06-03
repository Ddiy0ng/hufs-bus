package hufsbus.spring.domain.favorite.entity;

import hufsbus.spring.domain.auth.entity.User;
import hufsbus.spring.domain.favorite.favoriteEnum.DayEnum;
import hufsbus.spring.domain.timetable.entity.Timetable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@Entity
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayEnum day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Favorite of(DayEnum day, Timetable timetable, User user) {

        Favorite favorite = new Favorite();
        favorite.day = day;
        favorite.timetable = timetable;
        favorite.user = user;

        return favorite;
    }
}
