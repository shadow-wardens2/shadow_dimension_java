package Services.event;

import Entities.event.Reservation;
import Entities.event.ReservationStatus;

public interface ReservationNotificationGateway {
    void notifyReservationDecision(Reservation reservation, ReservationStatus newStatus);
}
