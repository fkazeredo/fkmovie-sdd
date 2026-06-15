package com.fksoft.application.api;

import com.fksoft.domain.booking.ReprintView;
import com.fksoft.domain.booking.TicketReprintService;
import com.fksoft.infra.security.UserContextProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator ticket reprint endpoint (SPEC-0020): audited, no state change. Restricted to OPERATOR/ADMIN
 * by the security chain; reprintability rules live in {@link TicketReprintService}.
 */
@RestController
@RequiredArgsConstructor
class OperatorTicketController {

    private final TicketReprintService reprintService;
    private final UserContextProvider userContext;

    @PostMapping("/api/operator/tickets/{ticketId}/reprint")
    ReprintView reprint(@PathVariable UUID ticketId) {
        return reprintService.reprint(ticketId, userContext.currentUser().userId());
    }
}
