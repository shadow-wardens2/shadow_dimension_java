package Services.event;

import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;

import java.sql.Timestamp;

public interface ReservationNotificationGateway {
    void notifyReservationDecision(Reservation reservation, ReservationStatus newStatus);

    void notifyEventReschedule(Reservation reservation, Event event, Timestamp oldStart, Timestamp oldEnd);
}
