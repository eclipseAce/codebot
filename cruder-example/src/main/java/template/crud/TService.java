package template.crud;

import io.cruder.apt.Template;
import io.cruder.example.core.BusinessErrors;
import io.cruder.example.core.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;
import template.crud.dto.TQueryDTO;

@Template
@Service
public class TService {
    @Autowired
    private TConverter converter;

    @Autowired
    private TRepository repository;

    @Transactional
    public Long add(TAddDTO add) {
        TEntity entity = converter.addToEntity(add);
        repository.save(entity);
        return entity.getId();
    }

    public TDetailsDTO get(Long id) {
        TEntity entity = repository.findById(id).orElse(null);
        if (entity == null) {
            throw new BusinessException(BusinessErrors.ENTITY_NOT_FOUND
                    .withMessage("#<path>(" + id + ") not found"));
        }
        return converter.entityToDetails(entity);
    }

    @Transactional
    public void delete(Long id) {
        TEntity TEntity = repository.findById(id).orElse(null);
        if (TEntity != null) {
            throw new BusinessException(BusinessErrors.ENTITY_NOT_FOUND
                    .withMessage("#<path>(" + id + ") not found"));
        }
        repository.delete(TEntity);
    }

    public Page<TListItemDTO> query(TQueryDTO body, Pageable pageable) {
        return repository.findAll(body.toPredicate(), pageable)
                .map(converter::entityToListItem);
    }
}
