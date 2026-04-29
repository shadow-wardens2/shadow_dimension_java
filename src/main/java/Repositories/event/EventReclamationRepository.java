package Repositories.event;

import Entities.event.EventReclamation;
import Entities.event.EventReclamationStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface EventReclamationRepository {
    EventReclamation create(EventReclamation reclamation) throws SQLException;

    Optional<EventReclamation> findById(int reclamationId) throws SQLException;

    Optional<EventReclamation> findOpenByUserAndEvent(int userId, int eventId) throws SQLException;

    List<EventReclamation> findByUser(int userId) throws SQLException;

    List<EventReclamation> findForBackOffice(String search, String status, String sortBy, boolean ascending, int offset, int limit) throws SQLException;

    int countForBackOffice(String search, String status) throws SQLException;

    void escalate(int reclamationId) throws SQLException;

    void adminRespond(int reclamationId, EventReclamationStatus status, String adminResponse) throws SQLException;

    void deleteById(int reclamationId) throws SQLException;
}
