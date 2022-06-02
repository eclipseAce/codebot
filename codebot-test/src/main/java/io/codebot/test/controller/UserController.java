package io.codebot.test.controller;

import io.codebot.test.dto.user.*;
import io.codebot.test.service.UserService;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService service;

    @PostMapping("/create")
    public long create(@RequestBody @Valid UserCreate dto) {
        return service.create(dto);
    }

    @PostMapping("/updatePassword/{id}")
    public void updatePassword(@PathVariable("id") long id,
                               @RequestBody @Valid UserUpdatePassword dto) {
        service.updatePassword(id, dto);
    }

    @PostMapping("/update/{id}")
    public UserDetails update(@PathVariable("id") long id,
                              @RequestBody @Valid UserUpdate dto) {
        return service.update(id, dto);
    }

    @GetMapping("/get/{id}")
    public UserDetails findById(@PathVariable("id") long id) {
        return service.findById(id);
    }

    @GetMapping("/getByUsername")
    public UserDetails findByUsername(@RequestParam("username") String username) {
        return service.findByUsername(username);
    }

    @PostMapping("/listAll")
    public List<UserSummary> findList(@RequestBody @Valid UserQuery query) {
        return service.findList(query);
    }

    @PostMapping("/page")
    @PageableAsQueryParam
    public Page<UserSummary> findPage(@RequestBody @Valid UserQuery query, Pageable pageable) {
        return service.findPage(query, pageable);
    }

    @GetMapping("/pageAll")
    @PageableAsQueryParam
    public Page<UserSummary> findAllPage(Pageable pageable) {
        return service.findAllPage(pageable);
    }
}
