package com.fksoft.application.cinema.api;

import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.RoomView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin room listing (SPEC-0007/0026): the rooms an admin picks from when scheduling a screening.
 * Restricted to ROLE_ADMIN by the security chain ({@code /api/admin/**}); the data comes from the
 * cinema module's own {@link CinemaCatalog} read API.
 */
@RestController
@RequestMapping("/api/admin/rooms")
class RoomAdminController {

    private final CinemaCatalog catalog;

    RoomAdminController(CinemaCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    List<RoomView> list() {
        return catalog.listRooms();
    }
}
