package org.bahmni_avni_integration.mapper.avni;

import org.bahmni_avni_integration.contract.avni.Enrolment;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSEncounter;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSEncounterProvider;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSFullEncounter;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSSaveObservation;
import org.bahmni_avni_integration.domain.*;
import org.bahmni_avni_integration.repository.MappingMetaDataRepository;
import org.bahmni_avni_integration.util.FormatAndParseUtil;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EnrolmentMapper {

    private final ObservationMapper observationMapper;
    private final MappingMetaDataRepository mappingMetaDataRepository;

    public EnrolmentMapper(ObservationMapper observationMapper, MappingMetaDataRepository mappingMetaDataRepository) {
        this.observationMapper = observationMapper;
        this.mappingMetaDataRepository = mappingMetaDataRepository;
    }

    public OpenMRSEncounter mapEnrolmentToEncounter(Enrolment enrolment, String patientUuid, String encounterTypeUuid, Constants constants) {
        OpenMRSEncounter openMRSEncounter = new OpenMRSEncounter();
        openMRSEncounter.setEncounterDatetime(FormatAndParseUtil.toISODateStringWithTimezone(new Date()));
        openMRSEncounter.setPatient(patientUuid);
        openMRSEncounter.setEncounterType(encounterTypeUuid);
        openMRSEncounter.setLocation(constants.getValue(ConstantKey.IntegrationBahmniLocation));
        openMRSEncounter.addEncounterProvider(new OpenMRSEncounterProvider(constants.getValue(ConstantKey.IntegrationBahmniProvider), constants.getValue(ConstantKey.IntegrationBahmniEncounterRole)));

        mapEnrolmentUuid(enrolment, openMRSEncounter);
        observationMapper.mapObservations((LinkedHashMap<String, Object>) enrolment.get("observations"), openMRSEncounter);
        return openMRSEncounter;
    }

    public OpenMRSEncounter mapEnrolmentToExistingEncounter(OpenMRSFullEncounter existingEncounter, Enrolment enrolment) {
        OpenMRSEncounter openMRSEncounter = new OpenMRSEncounter();
        openMRSEncounter.setUuid(existingEncounter.getUuid());
        var observations = observationMapper.updateOpenMRSObservationsFromAvniObservations(
                existingEncounter.getLeafObservations(),
                (Map<String, Object>) enrolment.get("observations"));
        openMRSEncounter.setObservations(observations);
        return openMRSEncounter;
    }

    private void mapEnrolmentUuid(Enrolment enrolment, OpenMRSEncounter openMRSEncounter) {
        MappingMetaData enrolmentUuidConcept = mappingMetaDataRepository.findByMappingGroupAndMappingType(MappingGroup.ProgramEnrolment, MappingType.EnrolmentUUID_Concept);
        openMRSEncounter.addObservation(OpenMRSSaveObservation.createPrimitiveObs(enrolmentUuidConcept.getBahmniValue(), enrolment.getUuid(), ObsDataType.Text));
    }
}