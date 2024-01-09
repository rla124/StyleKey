package com.thekey.stylekeyserver.stylepoint.service;

import com.thekey.stylekeyserver.exception.ErrorMessage;
import com.thekey.stylekeyserver.stylepoint.domain.StylePoint;
import com.thekey.stylekeyserver.stylepoint.dto.StylePointDto;
import com.thekey.stylekeyserver.stylepoint.repository.StylePointRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class StylePointAdminServiceImpl implements StylePointAdminService {

    private final StylePointRepository stylePointRepository;

    @Override
    public StylePoint findById(Long id) {
        return stylePointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorMessage.NOT_FOUND_STYLE_POINT.get() + id));
    }

    @Override
    public List<StylePoint> findAll() {
        return stylePointRepository.findAll();
    }

    @Override
    public StylePoint update(Long id, StylePointDto requestDto) {
        StylePoint stylePoint = stylePointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorMessage.NOT_FOUND_STYLE_POINT.get() + id));

        stylePoint.update(requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getImage());

        return stylePoint;
    }

}
