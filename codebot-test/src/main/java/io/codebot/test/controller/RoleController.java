package io.codebot.test.controller;

import io.codebot.test.dto.role.RoleCreate;
import io.codebot.test.dto.user.*;
import io.codebot.test.service.RoleService;
import io.codebot.test.service.UserService;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {
    @Autowired
    private RoleService service;

    @PostMapping("/create")
    public long create(@RequestBody @Valid RoleCreate dto) {
        return service.create(dto);
    }
}
