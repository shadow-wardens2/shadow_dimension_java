package Repositories.event;

import Entities.event.Reservation;
import Entities.event.ReservationStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Optional<Reservation> findByUserAndEvent(int userId, int eventId) throws SQLException;

    Optional<Reservation> findById(int reservationId) throws SQLException;

    Reservation createPending(int userId, int eventId) throws SQLException;

    void updateStatus(int reservationId, ReservationStatus status) throws SQLException;

    void delete(int reservationId) throws SQLException;

    List<Reservation> findAcceptedByUser(int userId) throws SQLException;

    List<Reservation> findForBackOffice(String search, String sortBy, boolean ascending, int offset, int limit) throws SQLException;

    int countForBackOffice(String search) throws SQLException;
}
