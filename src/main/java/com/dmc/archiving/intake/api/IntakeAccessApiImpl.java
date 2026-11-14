package com.dmc.archiving.intake.api;

import com.dmc.archiving.intake.repository.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IntakeAccessApiImpl implements IntakeAccessApi {

    private final IntakeRepository intakeRepository;

    IntakeAccessApiImpl(IntakeRepository intakeRepository) {
        this.intakeRepository = intakeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public IntakeAccessInfo accessFor(Long intakeId, Long userId) {
        if (intakeId == null) {
            return null;
        }
        return intakeRepository.findById(intakeId)
                .map(intake -> new IntakeAccessInfo(
                        intake.getId(),
                        intake.getTenantId(),
                        userId != null && intakeRepository.isOwnerOrAssigned(intakeId, userId)))
                .orElse(null);
    }
}
