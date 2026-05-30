package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.entity.Expertise;
import org.example.coursework3.entity.Specialist;
import org.example.coursework3.entity.SpecialistStatus;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.vo.SpecialistsDetailVo;
import org.example.coursework3.vo.SpecialistsExpertiseBriefVo;
import org.example.coursework3.vo.SpecialistsPageVo;
import org.example.coursework3.vo.SpecialistsVo;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SpecialistsInfoService {

    private final SpecialistsRepository specialistRepository;
    private final SlotRepository slotRepository;

    @Transactional(readOnly = true)
    public SpecialistsDetailVo getSpecialistDetail(String id) {
        Specialist specialist = specialistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("specialist not found: " + id));
        List<SpecialistsExpertiseBriefVo> expertise = specialist.getExpertises()
                .stream()
                .map(e -> new SpecialistsExpertiseBriefVo(e.getId(), e.getName()))
                .toList();

        return new SpecialistsDetailVo(
                specialist.getUserId(),
                specialist.getName(),
                specialist.getBio(),
                expertise,
                specialist.getPrice()
        );
    }

    @Transactional(readOnly = true)
    public SpecialistsPageVo getSpecialists(
            String expertiseId,
            String keyword,
            String date,
            BigDecimal maxPrice,
            boolean activeOnly,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        String normalizedExpertiseId = expertiseId == null ? "" : expertiseId.trim();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        LocalDate requestedDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);

        List<Specialist> specialists = normalizedExpertiseId.isBlank()
                ? specialistRepository.findAll()
                : specialistRepository.findDistinctByExpertises_Id(normalizedExpertiseId, Pageable.unpaged()).getContent();

        List<Specialist> filtered = specialists.stream()
                .filter(Objects::nonNull)
                .filter(specialist -> !activeOnly || specialist.getStatus() != SpecialistStatus.Inactive)
                .filter(specialist -> matchesKeyword(specialist, normalizedKeyword))
                .filter(specialist -> matchesMaxPrice(specialist, maxPrice))
                .filter(specialist -> matchesAvailableDate(specialist, requestedDate))
                .sorted(Comparator.comparing(Specialist::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        List<Specialist> pageContent = filtered.subList(fromIndex, toIndex);

        List<SpecialistsVo> items = new ArrayList<>();
        for (Specialist specialist : pageContent) {
            List<String> expertiseIds = new ArrayList<>();
            List<String> expertiseNames = new ArrayList<>();
            for (Expertise expertise : specialist.getExpertises()) {
                expertiseIds.add(expertise.getId());
                expertiseNames.add(expertise.getName());
            }

            items.add(new SpecialistsVo(
                    specialist.getUserId(),
                    specialist.getName(),
                    specialist.getStatus(),
                    expertiseIds,
                    expertiseNames,
                    specialist.getPrice()
            ));
        }

        return new SpecialistsPageVo(items, filtered.size(), safePage, safePageSize);
    }

    private boolean matchesKeyword(Specialist specialist, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        if (specialist.getName() != null && specialist.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        return specialist.getExpertises().stream()
                .map(Expertise::getName)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains(keyword));
    }

    private boolean matchesMaxPrice(Specialist specialist, BigDecimal maxPrice) {
        if (maxPrice == null) {
            return true;
        }
        if (specialist.getPrice() == null) {
            return false;
        }
        return specialist.getPrice().compareTo(maxPrice) <= 0;
    }

    private boolean matchesAvailableDate(Specialist specialist, LocalDate requestedDate) {
        if (requestedDate == null) {
            return true;
        }
        List<Slot> slots = slotRepository.findBySpecialistId(specialist.getUserId());
        return slots.stream().anyMatch(slot ->
                Boolean.TRUE.equals(slot.getAvailable()) &&
                        requestedDate.equals(slot.getStartTime().toLocalDate()));
    }
}
