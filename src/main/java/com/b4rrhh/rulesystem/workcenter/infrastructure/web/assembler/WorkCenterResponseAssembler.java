package com.b4rrhh.rulesystem.workcenter.infrastructure.web.assembler;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.rulesystem.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenter;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.WorkCenterContactResponse;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.WorkCenterListItemResponse;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.WorkCenterResponse;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

@Component("rulesystemWorkCenterResponseAssembler")
public class WorkCenterResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public WorkCenterResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public WorkCenterResponse toResponse(WorkCenterDetails details) {
        WorkCenter workCenter = details.workCenter();
        WorkCenterProfile profile = details.profile();

        return new WorkCenterResponse(
                workCenter.ruleSystemCode(),
                workCenter.workCenterCode(),
                workCenter.name(),
                workCenter.description(),
                workCenter.startDate(),
                workCenter.endDate(),
                workCenter.active(),
                profile.getCompanyCode(),
                PostalAddressResponse.ofOrEmpty(profile.getAddress())
        );
    }

    public WorkCenterListItemResponse toListItemResponse(WorkCenterDetails details) {
        WorkCenter workCenter = details.workCenter();
        WorkCenterProfile profile = details.profile();

        return new WorkCenterListItemResponse(
                workCenter.ruleSystemCode(),
                workCenter.workCenterCode(),
                workCenter.name(),
                profile.getCompanyCode(),
                profile.getAddress() == null ? null : profile.getAddress().townLine(),
                profile.getAddress() == null ? null : profile.getAddress().countryCode(),
                workCenter.active(),
                workCenter.startDate(),
                workCenter.endDate()
        );
    }

    /**
     * El literal del tipo de contacto se resuelve aqui, en la capa web, en el idioma de la
     * respuesta: el modelo de dominio ya no lo lleva y ningun caso de uso conoce el idioma
     * (ADR-052 §4; backend#27).
     */
    public WorkCenterContactResponse toContactResponse(String ruleSystemCode, WorkCenterContact contact, ResponseLanguage language) {
        String contactTypeName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, WorkCenterRuleEntityTypeCodes.CONTACT_TYPE,
                        contact.getContactTypeCode(), language.code())
                .orElse(null);

        return new WorkCenterContactResponse(
                contact.getContactNumber(),
                contact.getContactTypeCode(),
                contactTypeName,
                contact.getContactValue()
        );
    }
}