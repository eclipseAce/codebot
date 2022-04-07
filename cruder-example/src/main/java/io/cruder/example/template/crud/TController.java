package io.cruder.example.template.crud;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cruder.example.core.ApiResult;

@RestController
@RequestMapping("/api/user")
public class TController {

    @Autowired
    private TConverter conv;

    @Autowired
    private TRepository repo;

    @PostMapping("/add")
    public ApiResult<TId> add(TAddDTO body) {
        TEntity entity = conv.addToEntity(body);
        repo.save(entity);
        return new ApiResult<>("OK", null, entity.getId());
    }

    @GetMapping("/list")
    public ApiResult<List<TListItemDTO>> list() {
        return new ApiResult<>("OK", null, repo.findAll().stream()
                .map(conv::entityToListItem)
                .collect(Collectors.toList()));
    }
}